package com.tamvagbackend.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BCryptHash {
    public static void main(String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: BCryptHash <secret>");
        }

        System.out.println(new BCryptPasswordEncoder().encode(args[0]));
    }
}