package com.github.rnewson.couchdb.lucene;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

class Scanner {
    
    private BufferedReader reader;

    Scanner(InputStream in) {
        this(in, null);
    }

    Scanner(InputStream in, String charsetName) {
        try {
            if (charsetName == null) {
                reader = new BufferedReader(new InputStreamReader(in));
            } else {
                reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            }
        } catch (IOException e) {
            throw new RuntimeException("can't initialize reader", e);
        }
    }

    /* boolean hasNextLine() {
        return reader.ready();
    } */

    String nextLine() {
        try {
            return reader.readLine();
        } catch (IOException e) {
            throw new RuntimeException("can't read line", e);
        }
    }
}
