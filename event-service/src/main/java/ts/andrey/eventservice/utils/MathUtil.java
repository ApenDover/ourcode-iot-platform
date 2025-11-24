package ts.andrey.eventservice.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MathUtil {

    public int divideCeiling(int a, int b) {
        return (a + (b - 1)) / b;
    }

}
