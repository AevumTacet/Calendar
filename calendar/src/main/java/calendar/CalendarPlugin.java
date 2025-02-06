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
    
    private final HashMap<Integer, String> romanNumerals = new HashMap<Integer, String>() {{
        put(1, "I");
        put(2, "II");
        put(3, "III");
        put(4, "IV");
        put(5, "V");
        put(6, "VI");
        put(7, "VII");
        put(8, "VIII");
        put(9, "IX");
        put(10, "X");
        put(11, "XI");
        put(12, "XII");
    }};

    // 1. Método getOverworld() faltante
    private World getOverworld() {
        return Bukkit.getWorlds().stream()
                .filter(world -> world.getEnvironment() == World.Environment.NORMAL)
                .findFirst()
                .orElse(Bukkit.getWorlds().get(0));
    }

    // 2. Método ticksAHora() faltante
    private String ticksAHora(long ticks) {
        long tiempoAjustado = (ticks + 6000) % 24000;
        int horas = (int) (tiempoAjustado / 1000);
        int minutos = (int) ((tiempoAjustado % 1000) * 60 / 1000);
        return String.format("%02d:%02d", horas, minutos);
    }

    // 3. Clase RelojListener como inner class
    public class RelojListener implements Listener {
        @EventHandler
        public void onItemHeld(PlayerItemHeldEvent event) {
            Player player = event.getPlayer();
            // Verificar si el slot nuevo tiene un ítem y si es un reloj
            if (player.getInventory().getItem(event.getNewSlot()) != null && 
                player.getInventory().getItem(event.getNewSlot()).getType() == Material.CLOCK) {
                mostrarHoraReloj(player);
            }
        }
    }

    public void mostrarHoraReloj(Player jugador) {
        World mundo = getOverworld();
        long ticks = mundo.getTime();
        String hora = ticksAHora(ticks);

        int año = (diaTotal - 1) / 360 + 1;
        int mes = ((diaTotal - 1) % 360) / 30 + 1;
        int diaMes = (diaTotal - 1) % 30 + 1;

        String mensajeRaw = getConfig().getString("mensaje_reloj", "Día %dd%, Mes %mm%, Año %aaaa% | Hora: %hrs%:%min%")
                .replace("%dd%", String.valueOf(diaMes))
                .replace("%mm%", romanNumerals.get(mes)) // 4. Corregido a .get()
                .replace("%aaaa%", String.valueOf(año))
                .replace("%hrs%", hora.split(":")[0])
                .replace("%min%", hora.split(":")[1]);

        Component mensaje = MiniMessage.miniMessage().deserialize(mensajeRaw);
        jugador.sendActionBar(mensaje);
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        cargarDatos();
        iniciarTarea();
        Bukkit.getPluginManager().registerEvents(new RelojListener(), this);
        getLogger().info("Enabled calendar");
        
        // Tarea para actualizar reloj en mano
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

    private void iniciarTarea() {
        World world = getOverworld();

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            long currentTime = world.getTime() % 24000;

            if (!isCurrentDayChecked && currentTime >= MORNING_TIME) {
                isCurrentDayChecked = true;
                diaTotal++;
                getConfig().set("dias", diaTotal);
                saveConfig();
                announceDay();
                playDaySound();
            } else if (currentTime < MORNING_TIME) {
                isCurrentDayChecked = false;
            }

            ultimoTiempo = currentTime;
        }, 0L, 100);
    }

    private void announceDay() {
        int año = (diaTotal - 1) / 360 + 1;
        int mes = ((diaTotal - 1) % 360) / 30 + 1;
        int diaMes = (diaTotal - 1) % 30 + 1;

        String mensajeRaw = getConfig().getString("mensaje", "Día %dd%, Mes %mm%, Año %aaaa%")
                .replace("%dd%", String.valueOf(diaMes))
                .replace("%mm%", romanNumerals.get(mes))
                .replace("%aaaa%", String.valueOf(año));

        Component mensaje = MiniMessage.miniMessage().deserialize(mensajeRaw);
        Audience audiencia = Audience.audience(Bukkit.getOnlinePlayers());
        audiencia.sendActionBar(mensaje);
    }

    private void playDaySound() {
        String sonidoConfig = getConfig().getString("sound", "entity.experience_orb.pickup");
        float volumen = (float) getConfig().getDouble("volume", 1.0);
        float tono = (float) getConfig().getDouble("pitch", 1.5);

        try {
            Sound sonido = Sound.valueOf(sonidoConfig);
            for (Player jugador : Bukkit.getOnlinePlayers()) {
                jugador.playSound(jugador.getLocation(), sonido, volumen, tono);
            }
        } catch (IllegalArgumentException e) {
            getLogger().warning("¡Sonido no válido en config.yml: " + sonidoConfig);
        }
    }

    private void cargarDatos() {
        diaTotal = getConfig().getInt("dias", 1);
        ultimoTiempo = getConfig().getLong("ultimo_tiempo", -1);
    }
    
    @Override
    public void onDisable() {
        getConfig().set("dias", diaTotal);
        getConfig().set("ultimo_tiempo", ultimoTiempo);
        saveConfig();
    }
}