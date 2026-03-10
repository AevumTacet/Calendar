package calendar;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired by CalendarPlugin at the start of each new in-game month.
 * Any plugin can listen to this event with @EventHandler.
 *
 * Example usage in Lordship (or any other plugin):
 *
 *   @EventHandler
 *   public void onMonthEnd(MonthEndEvent event) {
 *       int month = event.getMonth();   // 1-12
 *       int year  = event.getYear();    // 1+
 *       int day   = event.getDayTotal();
 *   }
 */
public class MonthEndEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    /** The month that just ENDED (1–12). */
    private final int month;

    /** The year that contains the month that just ended. */
    private final int year;

    /** The absolute day counter at the moment this event fires. */
    private final int dayTotal;

    public MonthEndEvent(int month, int year, int dayTotal) {
        this.month    = month;
        this.year     = year;
        this.dayTotal = dayTotal;
    }

    /** @return The month that just ended (1–12). */
    public int getMonth() {
        return month;
    }

    /** @return The in-game year of the month that just ended. */
    public int getYear() {
        return year;
    }

    /** @return The absolute day total at the moment the event fired. */
    public int getDayTotal() {
        return dayTotal;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}