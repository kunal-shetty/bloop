package com.kun.pixelwords.generator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * Build-time tool that shrinks the raw word lists into the files shipped in the app's assets:
 * words.txt (all valid 3-7 letter words, offensive words removed) and common.txt (the subset people know).
 *
 * Usage: DictionaryPrep enable1.txt common.txt badwords.txt outDir
 */
public final class DictionaryPrep {

    public static void main(String[] args) throws IOException {
        if (args.length != 4) {
            System.err.println("usage: DictionaryPrep <enable1.txt> <common.txt> <badwords.txt> <outDir>");
            System.exit(2);
        }
        Dictionary dict;
        try (FileInputStream words = new FileInputStream(args[0]);
             FileInputStream common = new FileInputStream(args[1]);
             FileInputStream bad = new FileInputStream(args[2])) {
            dict = Dictionary.load(words, common, bad);
        }
        File out = new File(args[3]);
        if (!out.isDirectory() && !out.mkdirs()) throw new IOException("Cannot create " + out);
        write(new File(out, "words.txt"), dict.validWords());
        write(new File(out, "common.txt"), dict.commonWords());
        System.out.println("words.txt: " + dict.validWords().size() + " words, common.txt: " + dict.commonWords().size() + " words");
    }

    private static void write(File f, Iterable<String> lines) throws IOException {
        try (PrintWriter w = new PrintWriter(f, StandardCharsets.UTF_8.name())) {
            for (String s : lines) w.print(s + "\n");
        }
    }
}
