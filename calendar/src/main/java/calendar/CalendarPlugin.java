package calendar;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class CalendarPlugin extends JavaPlugin {

    private int diaTotal = 1;
    private long ultimoTiempo = -1;

    @Override
    public void onEnable() {
        saveDefaultConfig(); // Crea el config.yml si no existe
        cargarDatos();
        iniciarTarea();
        getLogger().info("¡Plugin Calendar activado!");
    }

    private void cargarDatos() {
        diaTotal = getConfig().getInt("dias", 1); // Carga días desde el config
    }

    private void iniciarTarea() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            World mundo = Bukkit.getWorlds().get(0);
            long tiempoActual = mundo.getTime() % 24000;

            if (ultimoTiempo != -1 && ultimoTiempo < 1000 && tiempoActual >= 1000) {
                diaTotal++;
                getConfig().set("dias", diaTotal);
                saveConfig();
                anunciarNuevoDia();
                reproducirSonido();
            }

            ultimoTiempo = tiempoActual;
        }, 0L, 1L);
    }

    private void anunciarNuevoDia() {
        int año = (diaTotal - 1) / 360 + 1;
        int mes = ((diaTotal - 1) % 360) / 30 + 1;
        int diaMes = (diaTotal - 1) % 30 + 1;

        // Formato con colores (usando MiniMessage)
        String mensajeRaw = getConfig().getString("mensaje", "&eDía %dia%, Mes %mes%, Año %año%")
                .replace("%dia%", String.valueOf(diaMes))
                .replace("%mes%", convertirARomano(mes))
                .replace("%año%", String.valueOf(año));

        Component mensaje = MiniMessage.miniMessage().deserialize(mensajeRaw);

        for (Player jugador : Bukkit.getOnlinePlayers()) {
            jugador.sendActionBar(mensaje);
        }
    }

    private void reproducirSonido() {
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

    private String convertirARomano(int numero) {
        return switch (numero) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            case 11 -> "XI";
            case 12 -> "XII";
            default -> Integer.toString(numero);
        };
    }

    @Override
    public void onDisable() {
        saveConfig();
        getLogger().info("¡Plugin Calendar desactivado!");
    }
}