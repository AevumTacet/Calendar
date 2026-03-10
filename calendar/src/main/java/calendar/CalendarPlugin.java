package calendar;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.HashMap;

public class CalendarPlugin extends JavaPlugin implements Listener {

    private int diaTotal = 1;
    private long ultimoTiempo = -1;
    private boolean isCurrentDayChecked = false;
    private final long MORNING_TIME = 100;

    // Tracks the last month we fired MonthEndEvent for, to avoid double-firing.
    private int lastFiredMonth = -1;

    private final HashMap<Integer, String> romanNumerals = new HashMap<Integer, String>() {{
        put(1, "I");   put(2, "II");  put(3, "III"); put(4, "IV");
        put(5, "V");   put(6, "VI");  put(7, "VII"); put(8, "VIII");
        put(9, "IX");  put(10, "X");  put(11, "XI"); put(12, "XII");
    }};

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private World getOverworld() {
        return Bukkit.getWorlds().stream()
                .filter(world -> world.getEnvironment() == World.Environment.NORMAL)
                .findFirst()
                .orElse(Bukkit.getWorlds().get(0));
    }

    private String ticksAHora(long ticks) {
        long tiempoAjustado = (ticks + 6000) % 24000;
        int horas   = (int) (tiempoAjustado / 1000);
        int minutos = (int) ((tiempoAjustado % 1000) * 60 / 1000);
        return String.format("%02d:%02d", horas, minutos);
    }

    /**
     * Returns the in-game month (1–12) for a given absolute day number.
     * Month changes every 30 days.
     */
    public static int getMonth(int dayTotal) {
        return ((dayTotal - 1) % 360) / 30 + 1;
    }

    /**
     * Returns the in-game year (1+) for a given absolute day number.
     * Year changes every 360 days (12 months × 30 days).
     */
    public static int getYear(int dayTotal) {
        return (dayTotal - 1) / 360 + 1;
    }

    /**
     * Returns the day-of-month (1–30) for a given absolute day number.
     */
    public static int getDayOfMonth(int dayTotal) {
        return (dayTotal - 1) % 30 + 1;
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void onEnable() {
        saveDefaultConfig();
        cargarDatos();
        iniciarTarea();
        Bukkit.getPluginManager().registerEvents(new RelojListener(), this);
        getLogger().info("Calendar enabled.");

        // Task to update clock display every second
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getInventory().getItemInMainHand().getType() == Material.CLOCK) {
                        mostrarHoraReloj(p);
                    }
                }
            }
        }.runTaskTimer(this, 0, 20);
    }

    @Override
    public void onDisable() {
        getConfig().set("dias", diaTotal);
        getConfig().set("ultimo_tiempo", ultimoTiempo);
        saveConfig();
    }

    // -------------------------------------------------------------------------
    // Day task
    // -------------------------------------------------------------------------

    private void iniciarTarea() {
        World world = getOverworld();

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            long currentTime = world.getTime() % 24000;

            if (!isCurrentDayChecked && currentTime >= MORNING_TIME) {
                isCurrentDayChecked = true;

                // Capture calendar state BEFORE incrementing day
                int prevDay   = diaTotal;
                int prevMonth = getMonth(prevDay);
                int prevYear  = getYear(prevDay);

                diaTotal++;
                getConfig().set("dias", diaTotal);
                saveConfig();

                announceDay();
                playDaySound();

                // ---- Fire month/year events when the month has turned ----
                // A month ends when the day-of-month of the new day resets to 1
                // AND we haven't already fired for this month.
                int newDayOfMonth = getDayOfMonth(diaTotal);
                if (newDayOfMonth == 1 && prevMonth != lastFiredMonth) {
                    lastFiredMonth = prevMonth;

                    // Fire MonthEndEvent (reports the month that just ended)
                    MonthEndEvent monthEvent = new MonthEndEvent(prevMonth, prevYear, diaTotal);
                    Bukkit.getPluginManager().callEvent(monthEvent);

                    // If the month was 12, a year also ended — fire YearEndEvent
                    if (prevMonth == 12) {
                        YearEndEvent yearEvent = new YearEndEvent(prevYear, diaTotal);
                        Bukkit.getPluginManager().callEvent(yearEvent);
                    }
                }

            } else if (currentTime < MORNING_TIME) {
                isCurrentDayChecked = false;
            }

            ultimoTiempo = currentTime;
        }, 0L, 100);
    }

    // -------------------------------------------------------------------------
    // Display helpers
    // -------------------------------------------------------------------------

    private void announceDay() {
        int año    = getYear(diaTotal);
        int mes    = getMonth(diaTotal);
        int diaMes = getDayOfMonth(diaTotal);

        String mensajeRaw = getConfig().getString("mensaje", "Day %dd%, Month %mm%, Year %aaaa%")
                .replace("%dd%", String.valueOf(diaMes))
                .replace("%mm%", romanNumerals.get(mes))
                .replace("%aaaa%", String.valueOf(año));

        Component mensaje = MiniMessage.miniMessage().deserialize(mensajeRaw);
        Audience audiencia = Audience.audience(Bukkit.getOnlinePlayers());
        audiencia.sendActionBar(mensaje);
    }

    @SuppressWarnings("removal")
    private void playDaySound() {
        String sonidoConfig = getConfig().getString("sound", "entity.experience_orb.pickup");
        float volumen = (float) getConfig().getDouble("volume", 1.0);
        float tono    = (float) getConfig().getDouble("pitch", 1.5);

        try {
            Sound sonido = Sound.valueOf(sonidoConfig);
            for (Player jugador : Bukkit.getOnlinePlayers()) {
                jugador.playSound(jugador.getLocation(), sonido, volumen, tono);
            }
        } catch (IllegalArgumentException e) {
            getLogger().warning("Invalid sound in config.yml: " + sonidoConfig);
        }
    }

    public void mostrarHoraReloj(Player jugador) {
        World mundo  = getOverworld();
        long ticks   = mundo.getTime();
        String hora  = ticksAHora(ticks);

        int año    = getYear(diaTotal);
        int mes    = getMonth(diaTotal);
        int diaMes = getDayOfMonth(diaTotal);

        String mensajeRaw = getConfig().getString("mensaje_reloj",
                        "Day %dd%, Month %mm%, Year %aaaa% | Hour: %hrs%:%min%")
                .replace("%dd%", String.valueOf(diaMes))
                .replace("%mm%", romanNumerals.get(mes))
                .replace("%aaaa%", String.valueOf(año))
                .replace("%hrs%", hora.split(":")[0])
                .replace("%min%", hora.split(":")[1]);

        Component mensaje = MiniMessage.miniMessage().deserialize(mensajeRaw);
        jugador.sendActionBar(mensaje);
    }

    // -------------------------------------------------------------------------
    // Persistence
    // -------------------------------------------------------------------------

    private void cargarDatos() {
        diaTotal      = getConfig().getInt("dias", 1);
        ultimoTiempo  = getConfig().getLong("ultimo_tiempo", -1);
        // Restore lastFiredMonth so a reload doesn't re-fire events
        lastFiredMonth = getMonth(diaTotal);
    }

    // -------------------------------------------------------------------------
    // Public API — useful for other plugins that need to read the calendar
    // -------------------------------------------------------------------------

    /** @return Current absolute day total. */
    public int getDiaTotal() { return diaTotal; }

    /** @return Current in-game month (1–12). */
    public int getCurrentMonth() { return getMonth(diaTotal); }

    /** @return Current in-game year (1+). */
    public int getCurrentYear() { return getYear(diaTotal); }

    /** @return Current day of month (1–30). */
    public int getCurrentDayOfMonth() { return getDayOfMonth(diaTotal); }

    // -------------------------------------------------------------------------
    // Inner listener (clock item)
    // -------------------------------------------------------------------------

    public class RelojListener implements Listener {
        @EventHandler
        public void onItemHeld(PlayerItemHeldEvent event) {
            Player player = event.getPlayer();
            if (player.getInventory().getItem(event.getNewSlot()) != null &&
                player.getInventory().getItem(event.getNewSlot()).getType() == Material.CLOCK) {
                mostrarHoraReloj(player);
            }
        }
    }
}