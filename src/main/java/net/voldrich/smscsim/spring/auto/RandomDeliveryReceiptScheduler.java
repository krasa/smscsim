package net.voldrich.smscsim.spring.auto;

import net.voldrich.smscsim.spring.DeliveryReceiptScheduler;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * Created by Mavo on 2.8.2014.
 */
@Component
public class RandomDeliveryReceiptScheduler implements DeliveryReceiptScheduler {

	private int minDelayMs = 0;

    private int randomDeltaMs =  5000;

    private Random deliveryRandom = new Random();

    @Override
    public long getDeliveryTimeMillis() {
        return System.currentTimeMillis() + minDelayMs + (int) (deliveryRandom.nextDouble() * randomDeltaMs);
    }

    public int getMinDelayMs() {
        return minDelayMs;
    }

    public void setMinDelayMs(int minDelayMs) {
        this.minDelayMs = minDelayMs;
    }

    public int getRandomDeltaMs() {
        return randomDeltaMs;
    }

    public void setRandomDeltaMs(int randomDeltaMs) {
        this.randomDeltaMs = randomDeltaMs;
    }
}
