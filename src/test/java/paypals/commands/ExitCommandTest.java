package paypals.commands;

import org.junit.jupiter.api.Test;


public class ExitCommandTest {

    @Test
    public void testIsExit() {
        ExitCommand exitCommand = new ExitCommand("");
        assert exitCommand.isExit() : "isExit() should return true for an ExitCommand";
    }

}
