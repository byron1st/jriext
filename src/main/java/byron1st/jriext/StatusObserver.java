package byron1st.jriext;

/**
 * Created by byron1st on 2016. 5. 6..
 */
public class StatusObserver implements Observer {
    @Override
    public void update(String message) {
        System.out.println(message);
    }
}
