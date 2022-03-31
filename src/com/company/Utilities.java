package com.company;

import org.opencv.core.Point;

import java.util.ArrayList;

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
}
