package com.halvaor.gamingknights;

public class User {

    private String eMail;
    private String firstName;
    private String lastName;
    private String houseNumber;
    private String postalCode;
    private String street;
    private String town;
    private String userID;

    public User(String eMail, String firstName, String lastName, String houseNumber, String postalCode, String street, String town, String userID) {
        this.eMail = eMail;
        this.firstName = firstName;
        this.lastName = lastName;
        this.houseNumber = houseNumber;
        this.postalCode = postalCode;
        this.street = street;
        this.town = town;
        this.userID = userID;
    }

    public String getEmail() {
        return eMail;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getHouseNumber() {
        return houseNumber;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getStreet() {
        return street;
    }

    public String getTown() {
        return town;
    }

    public String getUserID() {
        return userID;
    }
}
