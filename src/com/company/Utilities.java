package com.company;

import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.regex.Pattern;

public final class Utilities {
    public static boolean ContainsCoordinates(ArrayList<Point> container, Point point)
    {
        for (Point p: container
        ) {
            if(point.x==p.x && point.y==p.y)
            {return true;}
        }
        return  false;
    }
    public static String GetDirectory(String path)
    {
        String separator = "\\";
        String[] elements= path.split(Pattern.quote(separator));
        String output="";
        for(int i=0;i<elements.length-1;i++)
        {
            output+=elements[i]+separator;
        }
        return output;
    }
}
