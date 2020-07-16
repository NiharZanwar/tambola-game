import java.util.Random;

public class Test {
    public static void main(String[] args) {
        for(int i = 0; i < 6; i++) {
			int p = randInt(i*5 + 1, (i+1) * 5);
            // ticket.add(p) ;
            System.out.println(p);
		} 
    }


    private static int randInt(int min, int max) {	//method to generate random numbers
	    Random rand = new Random();
	    int randomNum = rand.nextInt((max - min) + 1) + min;
	    return randomNum;
	}
}