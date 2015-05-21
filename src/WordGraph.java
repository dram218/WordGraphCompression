import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Queue;

public class WordGraph {
	ArrayList<WordNode> nodes=new ArrayList<WordNode>();
	ArrayList<Edge> edges=new ArrayList<Edge>();
	double[][] edgeWeights;
	ArrayList<String> sentences;
	ArrayList<Double> sentencesSalience;
	ArrayList<String> events;
	ArrayList<Double> eventsSalience;
	ArrayList<String> phrases;
	ArrayList<Double> phrasesSalience;
	ArrayList<String> verbs;
	int docCount;
	double maxeventSalience=0;
	ArrayList<WordNode> sortedNodes=new ArrayList<WordNode>();
	public WordGraph(int docID){
			
		WordNode start=new WordNode("STARTNODE");
		WordNode end=new WordNode("ENDNODE");
		start.salience=0.5;
		end.salience=0.5;
		this.nodes.add(start);
		this.nodes.add(end);
		this.docCount=docID;
		this.sentences=new ArrayList<String>();
		this.verbs=new ArrayList<String>();
		
	}
	

	public void AddCandidateToGraph(String sentence,String candidate){
		double candiSalience=0;
		int index=candidate.lastIndexOf("#");
				candiSalience=Double.valueOf(candidate.substring(index+1));
				maxeventSalience=maxeventSalience>candiSalience?maxeventSalience:candiSalience;
				candidate=candidate.substring(0,index);
				String[] candiWords=candidate.split("\\s");
				int[] candiPosition=new int[candiWords.length];
		try{
				for(int i=0;i<candiWords.length;i++){
					index=candiWords[i].lastIndexOf("-");
					candiPosition[i]=Integer.valueOf(candiWords[i].substring(index+1));
					candiWords[i]=candiWords[i].substring(0,index);					
				}
				if(candiWords.length>2)
					this.verbs.add(candiWords[1]);
		}
		catch(Exception ee){
			return;	
		}
		
		this.sentences.add(sentence);

		String[] words=sentence.split("\\s");
		String curWord="";
		
		WordNode preNode=this.nodes.get(0);
		int preNodeNo=0;
		
		//add all edges to previousnodes
		ArrayList<WordNode> previousNodes=new ArrayList<WordNode>();
		ArrayList<Integer> previousNodesNo=new ArrayList<Integer>();
		previousNodes.add(this.nodes.get(0));
		previousNodesNo.add(0);
		
		
		for(int i=0;i<words.length;i++){
			curWord=words[i];
			
			if(WordGraphCompression.wordsList.containsKey(curWord)==false)
				WordGraphCompression.wordsList.put(curWord,1);
			
			int curWordNo=IsNewWord(curWord);
			

			int distance=DistanceOfWordToCandidate(i+1,candiPosition);
			
			if(curWordNo==-1){
				WordNode newNode=new WordNode(curWord);
				newNode.mainWord=WordGraphCompression.GetMainWord(curWord, WordGraphCompression.titles.get(docCount).chains);
				
				newNode.salience+=candiSalience*Math.exp(-1*Math.sqrt(distance)); ///2.0
				int newNodeID=this.nodes.size();
				this.nodes.add(newNode);

			
				for(int kkk=0;kkk<previousNodes.size();kkk++){
					Edge newEdge=new Edge(previousNodesNo.get(kkk), newNodeID); 
					if(kkk!=0)
						newEdge.distancesInSens+=1.0/(i+1-kkk);
					this.edges.add(newEdge);
				}
				previousNodes.add(newNode);
				previousNodesNo.add(newNodeID);
				
				
				Edge newEdge=new Edge(newNodeID, 1); 
				this.edges.add(newEdge);
				
			}
			else{
				
				for(int kkk=0;kkk<previousNodes.size();kkk++){
					int exist = EdgeIsExist(previousNodesNo.get(kkk), curWordNo);
					if (exist == -1) {
						if (LoopIsExist(previousNodesNo.get(kkk), curWordNo) == false) {
							Edge newEdge = new Edge(previousNodesNo.get(kkk), curWordNo);
							if(kkk!=0)
								newEdge.distancesInSens+=1.0/(i+1-kkk);
							this.edges.add(newEdge);
						} 
						else {
							WordNode newNode = new WordNode(curWord);
							newNode.mainWord=WordGraphCompression.GetMainWord(curWord, WordGraphCompression.titles.get(docCount).chains);
							
							for(int kkkk=2;kkkk<this.nodes.size();kkkk++){
								if (this.nodes.get(kkkk).wordString.equalsIgnoreCase(curWord))
									this.nodes.get(kkkk).salience+=candiSalience*Math.exp(-1*Math.sqrt(distance));
							}
							
							newNode.salience=this.nodes.get(curWordNo).salience;
							int newNodeID = this.nodes.size();
							this.nodes.add(newNode);
							curWordNo = newNodeID;
							
							
							Edge newEdge=new Edge(newNodeID, 1); 
							this.edges.add(newEdge);
							
							for(int kkkk=0;kkkk<previousNodes.size();kkkk++){
								newEdge = new Edge(previousNodesNo.get(kkkk), newNodeID);
							
								if(kkk!=0)
									newEdge.distancesInSens+=1.0/(i+1-kkkk);
								this.edges.add(newEdge);
							}
						}
					} 
					else {
						if(kkk!=0)
							this.edges.get(exist).distancesInSens += 1.0/(i+1-kkk);
					}
				}
				
				preNode = this.nodes.get(curWordNo);
				previousNodes.add(preNode);
				preNode.salience+=candiSalience*Math.exp(-1*Math.sqrt(distance));
				previousNodesNo.add(curWordNo);
				
				
				
			}
		}
	}
	
	public boolean LoopIsExist(int start,int end){
		if(start==end) return true;
		Queue<Integer> nodes=new LinkedList<Integer>();
		nodes.offer(end);
		ArrayList<Edge> tmp= new ArrayList<Edge>();
		for(int i=0;i<this.edges.size();i++){
			Edge ee=new Edge(this.edges.get(i).startNode, this.edges.get(i).endNode);
			tmp.add(ee);
		}
		while(nodes.isEmpty()==false){
			Integer curNode=nodes.poll();
			for(int kkk=0;kkk<tmp.size();){
				if(tmp.get(kkk).startNode==curNode){
					if(tmp.get(kkk).endNode==start) 
						return true;
					else{
						nodes.offer(tmp.get(kkk).endNode);
						tmp.remove(kkk);
					}
				}
				else {
					kkk++;
				}
			}
		}
		return false;
	}
	
	public int EdgeIsExist(int start,int end){
		for(int i=0;i<this.edges.size();i++){
			if(this.edges.get(i).startNode==start&&this.edges.get(i).endNode==end){
				return i;
			}
		}
		return -1;
	}
	
	public void NormalizedWordSal(){
		double totalWordSal=0;
		for(int i=0;i<this.nodes.size();i++){
			totalWordSal+=this.nodes.get(i).salience;
		}
			for(int i=0;i<this.nodes.size();i++){
			this.nodes.get(i).salience/=totalWordSal;
			WordNode tmp=WordGraphCompression.DeepCopyWordNode(this.nodes.get(i));
			this.sortedNodes.add(tmp);
		}
		
		Collections.sort(this.sortedNodes,new SortBySalience());
		if(this.sortedNodes.get(0).salience>this.sortedNodes.get(2).salience) System.out.println(">>>>>");
		else System.out.println("<<<<<<");
		
		
	}
	
	public void NormalizedEdgeWeight(){
		double totalEdgeWeight=0;
		for(int i=0;i<this.edges.size();i++){
			totalEdgeWeight+=this.edges.get(i).salience;
		}
		for(int i=0;i<this.edges.size();i++){
			this.edges.get(i).salience/=totalEdgeWeight;
		}
	}
	public int DistanceOfWordToCandidate(int wordPos,int[] candidatePos){
		int minDistance=50;
		int curDistance;
		for(int i=0;i<candidatePos.length;i++){
			curDistance=Math.abs(candidatePos[i]-wordPos);
			minDistance=minDistance<curDistance?minDistance:curDistance;
		}
		return minDistance;
	}
	
	public int IsNewWord(String curWord){
		for(int i=2;i<this.nodes.size();i++){
			if(this.nodes.get(i).wordString.equalsIgnoreCase(curWord))
				return i;
			if (WordGraphCompression.IsSameLexicalChain(this.nodes.get(i).wordString, curWord, docCount))
				return i;
		}
		return -1;
	}

	public void CalculateEdgeWeight(){
		
		this.edgeWeights=new double[this.nodes.size()][this.nodes.size()];
		for(int i=0;i<this.nodes.size();i++){
			for(int j=0;j<this.nodes.size();j++){
				this.edgeWeights[i][j]=-1;
			}
		}
		int start,end;
		for(int i=0;i<this.edges.size();){
			start=this.edges.get(i).startNode;
			end=this.edges.get(i).endNode;
			double startSal=this.nodes.get(start).salience;
			double endSal=this.nodes.get(end).salience;
			
			if(startSal<1e-10||endSal<1e-10){
				this.edges.remove(i);
				continue;
			}
			
			if(start==0||end==1){
				this.edges.get(i).salience=(startSal*endSal)/(startSal+endSal);
				i++;
			}
			else{
				this.edges.get(i).salience=(this.edges.get(i).distancesInSens*startSal*endSal)/(startSal+endSal);			
				if(this.edges.get(i).salience<1.0E-10){
					this.edges.remove(i);
				}
				else {
					i++;
				}
			}
		}
		
		for(int i=0;i<this.edges.size();i++){
			start=this.edges.get(i).startNode;
			end=this.edges.get(i).endNode;
			this.edgeWeights[start][end]=this.edges.get(i).salience;			
		}
	}
	
	
}

class SortBySalience implements Comparator<WordNode> {
	@Override
	public int compare(WordNode arg0, WordNode arg1) {
		// TODO Auto-generated method stub
		WordNode s1 = (WordNode) arg0;
		WordNode s2 = (WordNode) arg1;
		  if (s1.salience<s2.salience)
		   return 1;
		  else 
			  return -1;
	}
}
