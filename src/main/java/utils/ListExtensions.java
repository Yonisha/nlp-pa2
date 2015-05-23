package utils;

import java.util.ArrayList;
import java.util.List;

public class ListExtensions {

    public static <T> List<T> takeRight(List<T> list, int amountToTake){
        List<T> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            if (i >= list.size() - amountToTake)
                result.add(list.get(i));
        }

        return result;
    }
}
