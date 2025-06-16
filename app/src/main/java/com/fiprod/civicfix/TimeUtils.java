package com.fiprod.civicfix;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TimeUtils {

    public static String getRelativeTime(String isoDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            sdf.setLenient(false);
            Date past = sdf.parse(isoDate);
            Date now = new Date();

            long seconds = TimeUnit.MILLISECONDS.toSeconds(now.getTime() - past.getTime());
            long minutes = TimeUnit.MILLISECONDS.toMinutes(now.getTime() - past.getTime());
            long hours = TimeUnit.MILLISECONDS.toHours(now.getTime() - past.getTime());
            long days = TimeUnit.MILLISECONDS.toDays(now.getTime() - past.getTime());

            if (seconds < 60)
                return "just now";
            else if (minutes < 60)
                return minutes + " minutes ago";
            else if (hours < 24)
                return hours + " hours ago";
            else if (days < 7)
                return days + " days ago";
            else if (days < 30)
                return (days / 7) + " weeks ago";
            else if (days < 365)
                return (days / 30) + " months ago";
            else
                return (days / 365) + " years ago";

        } catch (ParseException e) {
            e.printStackTrace();
            return "unknown";
        }
    }
}
