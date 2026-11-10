package com.kun.pixelwords;

import java.util.ArrayList;
import java.util.List;

/** The words of the credits, privacy policy, terms and refund policy. Each is a list of titled sections. */
final class LegalText {

    static final String APP = "Paws & Puzzles: Word Connect";
    static final String UPDATED = "10 November 2026";

    enum Kind {
        CREDITS("CREDITS"), PRIVACY("PRIVACY POLICY"), TERMS("TERMS OF SERVICE"), REFUNDS("REFUND POLICY");

        final String title;

        Kind(String title) {
            this.title = title;
        }
    }

    static final class Section {
        final String heading;
        final String[] paragraphs;

        Section(String heading, String... paragraphs) {
            this.heading = heading;
            this.paragraphs = paragraphs;
        }
    }

    private LegalText() {}

    static List<Section> sections(Kind kind) {
        switch (kind) {
            case PRIVACY: return privacy();
            case TERMS: return terms();
            case REFUNDS: return refunds();
            default: return credits();
        }
    }

    private static List<Section> credits() {
        List<Section> s = new ArrayList<>();
        s.add(new Section("MADE WITH LOVE",
                APP + " was made for Ayushi, who inspired it, and for everyone who likes a quiet word puzzle and a cat that cheers them on.",
                "Every cat, boss, room, sound and animation in the game was drawn or built for it. If a cat ever makes your day a little better, that is the whole point."));
        s.add(new Section("THE GAME",
                "Design, art, code, level generation, sound effects and music were all made by the developer of this game.",
                "The sound effects and music are synthesised from scratch. The cat, its accessories and the bosses are drawn in code, and every Journey region and boss is generated, so none of them repeat."));
        s.add(new Section("THANK YOU TO OUR TESTERS",
                "Thank you to the friends and family who played early builds, found the bugs, and told the truth about what felt wrong. The game is better because of you."));
        s.add(new Section("EFFECTS",
                "PixelFX sprite sheets by patch-gg, released under the MIT licence, with parts adapted from Hugo Duprez's MIT-licensed effects (Sprite Fusion).",
                "MIT licence: permission is granted, free of charge, to use, copy, modify and distribute this material, provided the copyright and permission notice are kept with it. The material is provided as is, without warranty of any kind."));
        s.add(new Section("FONTS",
                "Press Start 2P and VT323 are used under the SIL Open Font License 1.1. Their licence text ships inside the app's files."));
        s.add(new Section("WORD LISTS",
                "The playable words come from the ENABLE word list, which is in the public domain.",
                "Common-word ranking uses google-10000-english by first20hours, derived from Google's Trillion Word Corpus.",
                "Offensive words are filtered out using the LDNOOBW list of dirty, naughty, obscene and otherwise bad words, licensed CC BY 4.0."));
        s.add(new Section("WORD MEANINGS",
                "Word meanings come from WordNet 3.1. WordNet is Copyright 2011 by Princeton University. All rights reserved.",
                "WordNet is used under its licence, which permits use, copying, modification and distribution of the database and its documentation for any purpose without fee or royalty, as long as this notice and the licence stay with it. THIS SOFTWARE AND DATABASE IS PROVIDED \"AS IS\" AND PRINCETON UNIVERSITY MAKES NO REPRESENTATIONS OR WARRANTIES, EXPRESS OR IMPLIED."));
        s.add(new Section("SERVICES",
                "Google Play Games Services provides sign-in, leaderboards and cloud save.",
                "Google AdMob provides the ads, and the Google User Messaging Platform provides the ad consent form.",
                "Google Play Billing handles purchases.",
                "Google, Google Play and Android are trademarks of Google LLC. Other names belong to their owners."));
        s.add(new Section("CONTACT",
                "Questions, bugs or ideas? Please " + Links.contact() + "."));
        return s;
    }

    private static List<Section> privacy() {
        List<Section> s = new ArrayList<>();
        s.add(new Section("SUMMARY",
                "Last updated: " + UPDATED + ".",
                APP + " (\"the game\", \"we\", \"us\") is a single-player word puzzle with a few optional features that use the internet or your local network. You do not need an account to play.",
                "In short: your progress is stored on your phone. We do not sell your personal information. Ads are shown by Google AdMob, which may collect device information as described below, and you can change your ad choices at any time in Settings."));
        s.add(new Section("WHAT STAYS ON YOUR DEVICE",
                "Your progress, coins, purchase records, settings, the name you choose for multiplayer, streaks and achievements are saved on your phone. We do not receive this information unless you turn on cloud save.",
                "The game never asks for your real name, address, phone number, contacts, photos or precise location."));
        s.add(new Section("CLOUD SAVE AND LEADERBOARDS",
                "If you choose to sign in with Google Play Games, your saved progress is stored in Google's cloud save for this game, and your scores can appear on leaderboards. This is handled by Google under Google's own privacy policy. We do not run our own servers and we do not see your Google account details beyond what the Play Games service gives the game, such as your player name and a player ID.",
                "You can sign out, and you can delete your Play Games data for this game, from the Google Play Games settings on your phone."));
        s.add(new Section("ADS",
                "The game shows ads provided by Google AdMob: short videos you choose to watch for a reward, and breaks between some levels. To show ads, Google may collect and use your advertising ID, IP address, device and app information and approximate location, and may use cookies or similar technologies.",
                "If you are in the European Economic Area, the United Kingdom or another region that requires it, you are asked for consent before personalised ads are used, and you can change that choice later under AD PRIVACY in Settings.",
                "You can reset or delete your advertising ID, or opt out of personalised ads, in your phone's Google settings. You can also remove ad breaks between levels by buying NO AD BREAKS in the store.",
                "More about how Google uses data: policies.google.com/technologies/partner-sites."));
        s.add(new Section("PURCHASES",
                "Coin packs and NO AD BREAKS are bought through Google Play Billing. Google processes your payment and keeps your payment details; we never see your card number. We receive a purchase record from Google so that we can deliver what you bought, and we keep a short, anonymous code of each purchase in your save so that a purchase is never counted twice."));
        s.add(new Section("MULTIPLAYER OVER WI-FI",
                "Races and duels with nearby friends run over your local Wi-Fi network. While you play, your chosen player name and your scores are sent directly to the other phones in the game, and the game may announce itself on your local network so friends can find it. Nothing is sent to an internet server for this feature."));
        s.add(new Section("NOTIFICATIONS",
                "Daily reminders are scheduled on your phone. They do not use a server. You choose whether they are on and when they arrive, and you can turn them off in Settings or in your phone's notification settings."));
        s.add(new Section("SHARING",
                "When you press Share for a daily result or your cat's room, the game creates text or a picture and opens your phone's share sheet. What you send, and to whom, is up to you."));
        s.add(new Section("CHILDREN",
                "The game is meant for a general audience and is not directed at children under 13 (or the minimum age in your country). We do not knowingly collect personal information from children. If you believe a child has given us information, please " + Links.contact() + " and we will help."));
        s.add(new Section("SECURITY AND RETENTION",
                "Your progress lives in your phone's private app storage. Uninstalling the game removes it, except for any cloud save you chose to keep with Google Play Games. We do not keep a copy of your progress on any server of our own, so there is nothing for us to delete on request; to remove cloud data use Google Play Games settings."));
        s.add(new Section("YOUR RIGHTS",
                "Depending on where you live, you may have rights over your personal information, such as access, correction, deletion and objection to certain processing. Because we hold almost no personal information about you, these are mostly exercised with Google, who run the services described above. You can always " + Links.contact() + " with a question."));
        s.add(new Section("CHANGES",
                "If we change this policy, the new version and its date will appear here, and the game will point it out when the change is important."));
        s.add(new Section("CONTACT",
                "To ask about privacy, please " + Links.contact() + "."));
        return s;
    }

    private static List<Section> terms() {
        List<Section> s = new ArrayList<>();
        s.add(new Section("AGREEMENT",
                "Last updated: " + UPDATED + ".",
                "These terms are the agreement between you and the developer of " + APP + ". By installing or playing the game you agree to them. If you do not agree, please do not use the game."));
        s.add(new Section("LICENCE",
                "We give you a personal, non-exclusive, non-transferable, revocable licence to install and play the game on your own devices for your own entertainment. You may not copy, sell, rent, modify, reverse engineer or extract parts of the game, except where the law says you may."));
        s.add(new Section("VIRTUAL COINS AND ITEMS",
                "Coins, hints, power-ups, accessories, furniture and other items in the game are virtual. They have no cash value, cannot be exchanged for money, and cannot be sold or traded outside the game. You buy a licence to use them in the game, not ownership of them.",
                "Coins have a maximum balance. We may adjust prices, rewards and the economy to keep the game fair and fun."));
        s.add(new Section("PURCHASES",
                "Real-money purchases are made through Google Play and are subject to Google Play's terms. Prices are shown in your local currency at the time of purchase. See the Refund Policy for what happens if something goes wrong."));
        s.add(new Section("ADS",
                "The game is free and supported by ads. You are free to watch rewarded videos or not. Ads are provided by third parties and we are not responsible for their content."));
        s.add(new Section("FAIR PLAY",
                "Please play fairly. Do not use cheats, modified apps, automation or exploits to gain coins, rewards or leaderboard places, and do not use offensive player names. We may reset scores or progress that came from cheating, and we may block access to online features."));
        s.add(new Section("GIFT CODES",
                "Gift codes give a one-time reward and work once per save. They may be withdrawn or expire, have no cash value, and may not be sold."));
        s.add(new Section("CLOUD SAVE",
                "Cloud save depends on Google Play Games. We try to keep your progress safe, but we cannot promise that it will never be lost, so please do not rely on it as your only copy of something important."));
        s.add(new Section("CHANGES TO THE GAME",
                "We update the game to fix problems and add things. We may change, add or remove features, events, rewards and modes, and we may stop the game one day. We will try to give notice of big changes."));
        s.add(new Section("OUR RESPONSIBILITY",
                "The game is provided \"as is\". We do our best, but we do not promise that it will always work, be free of errors or be available. To the fullest extent the law allows, we are not liable for indirect or consequential loss, or for loss of virtual items or progress. Nothing in these terms limits any right you have by law that cannot be limited, including consumer rights where you live."));
        s.add(new Section("ENDING THESE TERMS",
                "You can stop at any time by uninstalling the game. We may suspend or end your access if you break these terms."));
        s.add(new Section("CHANGES AND CONTACT",
                "We may update these terms, and the date above will change when we do. Continuing to play after an update means you accept it. To ask a question, please " + Links.contact() + "."));
        return s;
    }

    private static List<Section> refunds() {
        List<Section> s = new ArrayList<>();
        s.add(new Section("SUMMARY",
                "Last updated: " + UPDATED + ".",
                "All purchases in " + APP + " are made and paid through Google Play, so refunds are handled by Google Play under its own policy. We want you to be happy, and we will help however we can."));
        s.add(new Section("WHAT YOU CAN BUY",
                "Coin packs, which are used up when spent, and NO AD BREAKS, which is bought once and kept. Everything else in the game is earned or bought with coins, never with a second payment."));
        s.add(new Section("HOW TO ASK GOOGLE FOR A REFUND",
                "Open the Google Play Store, tap your profile picture, choose Payments and subscriptions, then Budget and history, find the purchase, and choose Report a problem or Request a refund. You can also visit support.google.com/googleplay and search for \"refund\".",
                "Google usually lets you ask for a refund of an in-app purchase within 48 hours, and decides each request. After that, please contact us and we will look at it fairly."));
        s.add(new Section("WHEN WE WILL HELP",
                "We will always help if you paid and did not get what you bought, if you were charged twice by mistake, or if a purchase was made on your account without your knowledge, by a child or by accident. Coins from a pack that did not arrive are added to your save once we have confirmed the purchase."));
        s.add(new Section("COINS ALREADY SPENT",
                "Coins that were spent on items cannot be returned, and a refund of a coin pack may remove those coins from your save. If a refund leaves your balance short, the balance can go down to zero, never below."));
        s.add(new Section("PENDING PAYMENTS",
                "If your payment is pending, nothing is lost. The purchase is added as soon as Google confirms it, and there is no need to pay again."));
        s.add(new Section("RESTORING PURCHASES",
                "NO AD BREAKS and your coin purchase records are checked every time the game opens, so reinstalling on the same Google account brings them back. Cloud save keeps your progress across phones."));
        s.add(new Section("YOUR LEGAL RIGHTS",
                "This policy does not take away any right you have under the law where you live, including the right to cancel a digital purchase where one applies."));
        s.add(new Section("CONTACT",
                "If you have a problem with a purchase, please " + Links.contact() + ". Tell us what you bought, roughly when, and the order number from your Google Play receipt (it starts with GPA)."));
        return s;
    }
}
