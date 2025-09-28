package sisicat.events;

import com.darkmagician6.eventapi.events.Event;

public class KeyEvent implements Event {

    private final int keyCode;
    private final int scanCode;
    private final int action;
    private final int modifiers;
    private final int codePoint;

    public KeyEvent(int keyCode, int scanCode, int action, int modifiers)
    {
        this.keyCode = keyCode;
        this.scanCode = scanCode;
        this.action = action;
        this.modifiers = modifiers;
        this.codePoint = 0;
    }

    public KeyEvent(int codePoint)
    {
        this.keyCode = 0;
        this.scanCode = 0;
        this.action = 0;
        this.modifiers = 0;
        this.codePoint = codePoint;
    }

    public int getKeyCode()
    {
        return keyCode;
    }

    public int getCodePoint()
    {
        return this.codePoint;
    }

    public int getAction()
    {
        return action;
    }

    public int getModifiers()
    {
        return modifiers;
    }

}
