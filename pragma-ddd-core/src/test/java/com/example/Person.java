package com.example;

import org.morecup.pragmaddd.core.annotation.AggregateRoot;
import org.morecup.pragmaddd.core.annotation.OrmField;

@AggregateRoot
public class Person {
    @OrmField(columnName = "name")
    private String name;
    @OrmField(columnName = "age")
    private int age;

    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public void updateName(String newName) {
        this.name = newName;
    }

    public String getName() {
        return this.name;
    }
}