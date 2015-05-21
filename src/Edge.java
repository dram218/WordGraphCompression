import java.util.ArrayList;


public class Edge {
	int startNode;
	int endNode;
	double salience;
	double distancesInSens;
	public Edge(int start,int end){
		this.startNode=start;
		this.endNode=end;
		this.salience=0;
		distancesInSens=0;
	}
}
