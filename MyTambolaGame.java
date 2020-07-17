
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Random;

class MyDealer implements Runnable {

    private MyGameData gameData;
    private boolean maxDrawsReached = false; // flag to determine that game has reached maximum rounds

    public MyDealer(MyGameData gameData) { // constructor
        this.gameData = gameData;
    }

    // function to check if any of the players have succeded
    public boolean hasPlayerSucceded(boolean[] playerSuccess) {
        boolean response = false;
        for (int i = 0; i < playerSuccess.length; i++) {
            if (playerSuccess[i]) {
                response = true;
            }
        }
        return response;
    }

    // function to check whether all players have completed their chance
    public boolean hasChanceCompleted(boolean[] playerChance) {
        boolean response = true;
        for (int i = 0; i < playerChance.length; i++) {
            if (!playerChance[i]) {
                response = false;
            }
        }
        return response;
    }

    private static int randInt(int min, int max) {
        Random rand = new Random();
        int randomNum = rand.nextInt((max - min) + 1) + min;
        return randomNum;
    }

    public void run() {
        while (true) {
            synchronized (gameData.lock1) { // aqquire lock on gameData object

                // check if any of the player has succeded
                if (this.hasPlayerSucceded(gameData.playerSuccessFlag)) {

                    gameData.isGameComplete = true;
                    gameData.lock1.notifyAll();
                    System.out.println("Dealer has found some player succeded");
                    break;

                    // check if all players have completed their chances
                } else if (this.hasChanceCompleted(gameData.playerChanceFlag)) {
                    gameData.noAnnouncedFlag = false;
                    for (int i = 0; i < gameData.playerChanceFlag.length; i++) {
                        gameData.playerChanceFlag[i] = false;
                    }

                    // check if maximum rounds have reached, if so break out of the dealer loop
                    if (gameData.currentDraw > 9) {
                        gameData.isGameComplete = true; // declare game complete if maximum draws have been reached
                        this.maxDrawsReached = true; // set flag
                        gameData.lock1.notifyAll();
                        System.out.println("Maximum draws reached ");
                        break;
                    }

                    System.out.println("\nAnnounce new number for this round -");
                    while (true) { // loop to validate and take input from the user (as dealer)
                        try {
                            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

                            int i = Integer.parseInt(br.readLine()); // for Integer Input
                            if (i > 50 || i < 0) {
                                System.out.println("plaease enter number from 0-50");
                                continue;
                            }
                            gameData.announcedNumbers.add(i);
                            break;
                        } catch (IOException ioe) {
                            System.out.println("Please enter number and not string");
                            continue;
                        }
                    }

                    // gameData.announcedNumbers.add(randInt(0, 50)); // generate a new number and
                    // add to the announcedNumbers ArrayList
                    gameData.noAnnouncedFlag = true; // declare that a new number has been generated
                    gameData.currentDraw += 1; // increase the round count

                    System.out.println("\nRound Number - " + gameData.currentDraw + ", number announced by Dealer - "
                            + gameData.announcedNumbers.get(gameData.announcedNumbers.size() - 1));

                    try {
                        Thread.sleep(1000); // thread sleeps for 1s after every chance
                    } catch (Exception e) {
                        System.out.println("interrupted during sleep MyDealer");
                    }

                    // notify all threads that a new number has been generated
                    gameData.lock1.notifyAll();
                    try {
                        gameData.lock1.wait(); // release lock on gameData
                    } catch (Exception e) {
                        System.out.println("exception caught while lock1.wait in dealer");
                    }
                }
            }
        }

        // when above while loop breaks out check what is the reason for dealer to
        // finish game
        if (this.maxDrawsReached) {
            System.out.println("Dealer is exiting because max draws have reached");
        } else {
            System.out.println("Dealer thread has exited loop");
        }
    }
}

/**
 * MyPlayer
 */
class MyPlayer implements Runnable {

    private int id; // playerID
    private MyGameData gameData;

    private final static int MAXNO = 10; // Maximum numbers on the ticket of the player

    private ArrayList<Integer> numbersFound; // ArrayList that holds the numbers that have bee striked out by the player
    private ArrayList<Integer> ticket = new ArrayList<Integer>(); // Players ticket

    public MyPlayer(MyGameData gameData, int id) { // constructor

        this.id = id;
        this.gameData = gameData;
        this.numbersFound = new ArrayList<Integer>();

        System.out.print("\nticket for player " + this.id + "------");
        for (int i = 0; i < MAXNO; i++) { // generate random list of the numbers on the ticket
            int p = randInt(0, 50);
            ticket.add(p);
            System.out.print("," + p);
        }
    }

    private static int randInt(int min, int max) { // method to generate random numbers
        Random rand = new Random();
        int randomNum = rand.nextInt((max - min) + 1) + min;
        return randomNum;
    }

    public void run() {
        while (true) {
            synchronized (gameData.lock1) { // aquire lock on gameData object

                // check whether game is not complete and new number has been announced and
                // player has not already played.
                if (!gameData.isGameComplete && gameData.noAnnouncedFlag && !gameData.playerChanceFlag[this.id]) {

                    ListIterator<Integer> ticketItr = ticket.listIterator();
                    boolean numberOnTicket = false; // boolean to determine whether number was found on the ticket
                    boolean numberAlreadyFound = false; // boolean to determine number was not already found

                    while (ticketItr.hasNext()) { // iterate through the player ticket

                        // check whether number on the ticket is equal to current number announced by
                        // the dealer
                        if (ticketItr.next() == gameData.announcedNumbers.get(gameData.currentDraw)) {

                            numberOnTicket = true; // set true because number was found on the ticket
                            ListIterator<Integer> announcedIterator = this.numbersFound.listIterator();

                            while (announcedIterator.hasNext()) { // iterate through the numbers already found by the
                                                                  // player in previous draws

                                // check if number in current draw is equal to the numbers already striked off
                                // by the player in previous draws
                                if (gameData.announcedNumbers.get(gameData.currentDraw) == announcedIterator.next()) {

                                    numberAlreadyFound = true; // set true if number in current draw is equal to any
                                                               // number previously found by the player

                                    break;
                                } else {
                                    continue;
                                }
                            }
                        }
                    }

                    // check for both the conditions before adding number to the numbersFound array
                    if (numberOnTicket && !numberAlreadyFound) {
                        this.numbersFound.add(gameData.announcedNumbers.get(gameData.currentDraw));
                        System.out.println("Player " + this.id + " found "
                                + gameData.announcedNumbers.get(gameData.currentDraw) + " on ticket");
                    }

                    gameData.playerChanceFlag[this.id] = true; // player declare that his chance is complete

                    // players checks that he has found 3 numbers then declare that he has won if
                    // condition true
                    if (numbersFound.size() >= 3) {
                        gameData.playerSuccessFlag[this.id] = true;
                        gameData.succededPlayers.add(this.id);
                    }

                    // notify other threads that this player has finished has chance
                    gameData.lock1.notifyAll();
                    try {
                        gameData.lock1.wait(); // give up lock on gameData and wait till some other thread calls
                                               // notifyAll()
                    } catch (Exception e) {
                        System.out.println("exception caught while lock1.wait in thread" + this.id);
                    }

                } else {

                    if (gameData.isGameComplete) { // check if game has completed, if so break out of loop in turn
                                                   // killing this loop
                        System.out.println("Player " + this.id + " has finished his game");
                        break;

                    } else {
                        // notify other threads that this player has finished has chance
                        gameData.lock1.notifyAll();

                        try {
                            gameData.lock1.wait(); // give up lock on gameData and wait till some other thread calls
                                                   // notifyAll()
                        } catch (Exception e) {
                            System.out.println("exception caught while lock1.wait in thread" + this.id);
                        }
                    }
                }
            }
        }
    }
}

class MyGameData {

    public ArrayList<Integer> announcedNumbers = new ArrayList<Integer>(); // All the numbers announced by dealer
    public boolean isGameComplete = false; // Flag that describes State of game - Complete (or) In-progress
    public boolean noAnnouncedFlag = false; // Flag set true by dealer to indicate that a new number has been announced
    public boolean[] playerSuccessFlag; // boolean array that players use to declare that they have won the game
    public boolean[] playerChanceFlag; // boolean array that players use to deeclare that they have completed their
                                       // chance
    public int MAX_DRAWS = 10; // Number of rounds for which the game will go on.
    public int currentDraw = -1; // Number that dealer sets to declare to players which round of game is going
                                 // on.
    public ArrayList<Integer> succededPlayers = new ArrayList<Integer>(); // Array that a player uses when he has
                                                                          // succeeded to add his id.
    public Object lock1 = new Object(); // object used by Dealer and players to aqquire lock on gameData

    MyGameData(int numberPlayers) { // Constructor
        this.playerSuccessFlag = new boolean[numberPlayers];
        this.playerChanceFlag = new boolean[numberPlayers];

        for (int i = 0; i < playerSuccessFlag.length; i++) { // set all player success false initially
            playerSuccessFlag[i] = false;
        }

        for (int i = 0; i < playerChanceFlag.length; i++) { // set all player chance true initially
            playerChanceFlag[i] = true;
        }
    }
}

public class MyTambolaGame {
    public static void main(String[] args) {

        int numberOfPlayers = 5;

        // USE OF COLLECTIONS
        final ArrayList<MyPlayer> players = new ArrayList<MyPlayer>(); // arraylist of all players
        final ArrayList<Thread> playerThreads = new ArrayList<Thread>(); // arraylist of all player threads

        final MyGameData game = new MyGameData(numberOfPlayers); // declare game data object and initiate for the number
                                                                 // of players

        final MyDealer dealer = new MyDealer(game); // Dealer object
        Thread dealerThread = new Thread(dealer); // dealer thread

        // generate objects and threads for all the players
        for (int i = 0; i < numberOfPlayers; i++) {
            players.add(new MyPlayer(game, i));
            playerThreads.add(new Thread(players.get(i)));
        }

        // Start the dealer thread
        dealerThread.start();

        // Start all player threads
        for (int i = 0; i < numberOfPlayers; i++) {
            playerThreads.get(i).start();
        }

        try { // wait for the dealer thread to finish
            dealerThread.join();

            // wait for all the player threads to finish
            for (int i = 0; i < numberOfPlayers; i++) {
                playerThreads.get(i).join();
            }

        } catch (Exception e) {
            System.out.println("error while calling join on dealer thread");
        }

        System.out.println("Dealer and players have exited the game");

        // declare results
        System.out.println("----------RESULTS----------");

        for (int i = 0; i < game.playerSuccessFlag.length; i++) {
            if (game.playerSuccessFlag[i]) {
                System.out.println("Player " + i + " has won the game");
            }
        }

        if (game.succededPlayers.size() == 0) {
            System.out.println("None of the players won the game !");
        } else {
            System.out.println("---------------------------");

            for (int i = 0; i < game.succededPlayers.size(); i++) {
                System.out.println("Player to report success at Position " + (i + 1) + " is Player - "
                        + game.succededPlayers.get(i));
            }
        }
    }
}