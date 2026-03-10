package calendar;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired by CalendarPlugin at the start of each new in-game year.
 * Fires immediately after the MonthEndEvent for month 12.
 *
 * Example usage:
 *
 *   @EventHandler
 *   public void onYearEnd(YearEndEvent event) {
 *       int year = event.getYear();  // the year that just ended
 *   }
 */
public class YearEndEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    /** The year that just ENDED. */
    private final int year;

    /** The absolute day counter at the moment this event fires. */
    private final int dayTotal;

    public YearEndEvent(int year, int dayTotal) {
        this.year     = year;
        this.dayTotal = dayTotal;
    }

    /** @return The in-game year that just ended. */
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