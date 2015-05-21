import java.util.ArrayList;

import jdk.internal.dynalink.beans.StaticClass;


public class Beam implements Cloneable{
	static int BeamSize=8;
	MinHeap heap;
	
	public Beam(){
		heap=new MinHeap(BeamSize);
		
	}

    public Object clone() throws CloneNotSupportedException
    {
        Object object = super.clone();
        return object;
    }
}
