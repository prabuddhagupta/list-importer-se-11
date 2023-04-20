package com.rdc.core.nameparser;

public interface PersonNameParser {

    PersonName parseName(String name);

    String formatName(String... namePieces);

    String formatName(PersonName personName);

}

