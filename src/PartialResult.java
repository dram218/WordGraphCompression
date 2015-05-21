import java.util.ArrayList;


public class PartialResult implements Cloneable{
	ArrayList<Integer> nodes=new ArrayList<Integer>();
	double score;
	double fluencyscore;
	double pathscore;
	double coveragescore;
	public PartialResult(){
		this.nodes.add(0);
		score=0;
		this.fluencyscore=0;
		this.pathscore=0;
		this.coveragescore=0;				
	}
	
		
	@Override
    public Object clone() throws CloneNotSupportedException
    {
        Object object = super.clone();
        return object;
    }
}
