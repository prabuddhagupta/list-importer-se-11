package com;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateTest {
    public static void main(String[] args) {
        String string = "28 October, 2018";
        DateTimeFormatter format = DateTimeFormatter.ofPattern("d MMMM, yyyy");

        LocalDate date = LocalDate.parse(string, format);
        System.out.println(date);

    }
}
