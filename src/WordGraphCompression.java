import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;

import com.sun.xml.internal.fastinfoset.algorithm.BuiltInEncodingAlgorithm.WordListener;


public class WordGraphCompression {

	static Hashtable<String,NGramScore> nGramList=new Hashtable<String,NGramScore>();
	static Hashtable<String,Integer> wordsList=new Hashtable<String,Integer>();
	static double ALPHA=0.1;
	
	static ArrayList<NewsContent> titles;
	static WordGraph graph;
	static String filePath="eventResult/";

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		if((new File("nGramList.obj")).exists()){
			nGramList=(Hashtable<String,NGramScore>)DeSerielizeObject("nGramList.obj");
		}
		else {
			CreateNgramModel("giga_3gram.lm");
		}
		
		titles=(ArrayList<NewsContent>)DeSerielizeObject("LexicalChainList.obj");
		System.out.println("ok!");
		
		FileWriter outFileWriter2=new FileWriter("mscprocess.txt",false);
		
		File[] files;
		String sourcefile = "";
		String titleString="";
		files = new File(filePath).listFiles();		
		FileWriter outFileWriter=new FileWriter("lastPathCandidates.txt",false);
		for (int i = 0; i < files.length; i++) {
			if (files[i].isFile()) {
				sourcefile = files[i].getName();
				
				titleString=CompressEachText(sourcefile,i,outFileWriter2);
				System.out.println("NO."+i+":"+sourcefile+"title over! ");
				outFileWriter.write(titleString+"\n");
				outFileWriter.flush();
			}
		}
		outFileWriter.flush();
		outFileWriter.close();


		outFileWriter2.flush();
		outFileWriter2.close();
	}
	

	public static boolean IsSameLexicalChain(String word1,String word2,int docID){
		
		LexicalChain chain;
		boolean found1=false,found2=false;
		
		for(int i=0;i<titles.get(docID).chains.size();i++){
			chain=titles.get(docID).chains.get(i);
			for(int j=0;j<chain.lexicalList.size();j++){		
				String lexicalString=chain.lexicalList.get(j).wordList.get(chain.lexicalList.get(j).wordList.size()-1);
				if(lexicalString.equalsIgnoreCase(word1)){	
					found1=true;
					for(int k=0;k<chain.lexicalList.size();k++){		
						lexicalString=chain.lexicalList.get(k).wordList.get(chain.lexicalList.get(k).wordList.size()-1);
						if(lexicalString.equalsIgnoreCase(word2))
							found2=true;
						else {
							found2=false;
						}
					}
					if(found1&&found2){
						return true;
					}
					return false;					
				}
				else {
					found1=false;
				}
			}
		}		
		return false;
	}
	

	public static String CompressEachText(String sourcefile,int docCount,FileWriter outFileWriter) throws Exception{
		String[] sent = null;
		
		double totalCoveragescore=0;
		for(int kk=0;kk<titles.get(docCount).chains.size();kk++){
			totalCoveragescore+=titles.get(docCount).chains.get(kk).salientScore;
		}
		
		
		wordsList.clear();
		
		graph=new WordGraph(docCount);
		
	
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath + sourcefile),"utf-8"));
		String buf = "";
		String sentence="";
		String candidate="";
		int eventcount=0;
		while ((buf = br.readLine()) != null) {
			buf = buf.trim();
			if(buf.length()==0)
				continue;
			candidate=buf;
			
			buf=br.readLine();
			buf=buf.trim();
			sentence=buf;
			
			outFileWriter.write("sentence:"+sentence+"\n");
			outFileWriter.write("candidate:"+candidate+"\n");
			graph.AddCandidateToGraph(sentence,candidate);

			eventcount++;
			if(eventcount==10)
				break;
		}				
		br.close();
		

		graph.NormalizedWordSal();

		graph.CalculateEdgeWeight();


		Beam agendas=new Beam();
		PartialResult ss=new PartialResult();
		agendas.heap.AddNew(ss);
		double pathScore,fluenceScore,coverageScore;
		double rankScore;
		System.out.println("nodeCount:"+graph.nodes.size());
		
		outFileWriter.write("maxeventsalience:"+graph.maxeventSalience+"\n");
		for(int kkkk=0;kkkk<graph.nodes.size();kkkk++){
			outFileWriter.write(String.valueOf(kkkk+1)+graph.nodes.get(kkkk).wordString+"_"+graph.nodes.get(kkkk).salience+"\n");
		}
		
		while(true){
			Beam agenda_next=new Beam();	

			
			for(int presult=0;presult<agendas.heap.size;presult++){
				
				PartialResult a = agendas.heap.array[presult];
				int preNode=a.nodes.get(a.nodes.size()-1);
				
				if(graph.nodes.get(preNode).wordString.equals("ENDNODE")){
					PartialResult newPartialResult=	DeepCopyPartialResult(a);
				
					agenda_next.heap.AddNew(newPartialResult);	
					
				}
				else{
				
					for(int i=0;i<graph.nodes.size();i++){					
						int curNode=i;
						if(graph.nodes.size()>52){
							if(graph.nodes.get(curNode).salience<0.01&&graph.nodes.get(curNode).salience<graph.sortedNodes.get(Math.round(graph.nodes.size()*0.5f)).salience)
								continue;
						}
						else{
							if(graph.nodes.get(curNode).salience<graph.sortedNodes.get(Math.round(graph.nodes.size()*0.7f)).salience) continue;
						}
						
						
						
						if(graph.edgeWeights[preNode][curNode]>=0){
							PartialResult newPartialResult=	DeepCopyPartialResult(a);
							
							newPartialResult.nodes.add(curNode);
							
							if(curNode==1&&HasVerb(newPartialResult)==false)	continue;
							if(curNode==1&&newPartialResult.nodes.size()<8) continue;
							if(newPartialResult.nodes.size()>12) continue;
							
							fluenceScore = CalFluencyScore(newPartialResult);//fluencescore

/*							int wordsize;
							if(curNode!=1) wordsize=a.nodes.size()-1;
							else wordsize=a.nodes.size()-2;
							
							double increfluscore=IncrementFluencyScore(newPartialResult);
							System.out.println("increfluescore:"+increfluscore);
							fluenceScore=(newPartialResult.fluencyscore*wordsize + increfluscore)/(wordsize+1);
*/								

							pathScore = CalPathScore(newPartialResult);	
								
/*							double increpathscore=graph.edgeWeights[preNode][curNode];
							System.out.println("increpathscore:"+increpathscore);
							pathScore = (newPartialResult.pathscore*wordsize+increpathscore)/(wordsize+1);
*/							
								//rankscore
							rankScore = CalTotalScore(fluenceScore, pathScore, newPartialResult);

							
							newPartialResult.score=rankScore;
							newPartialResult.fluencyscore=fluenceScore;
							newPartialResult.pathscore=pathScore;
							
							agenda_next.heap.AddNew(newPartialResult);							
						}					
						
					}
				}	
			}
			
			agendas=agenda_next;
			
			agenda_next.heap.Sort();
			//System.out.println("agenda_next over!");
			for(int kk=0;kk<agenda_next.heap.size;kk++)
				outFileWriter.write(OutputPartialResult(agenda_next.heap.array[kk])+"\n");
			outFileWriter.write("this turn is over!\n");
			outFileWriter.flush();
			//System.out.println("this turn is over!");
		
			

			boolean isOver=true;

			for(int kk=0;kk<agenda_next.heap.size;kk++){
					WordNode endNode=graph.nodes.get(agenda_next.heap.array[kk].nodes.get(agenda_next.heap.array[kk].nodes.size()-1));
					if(endNode.wordString.equals("ENDNODE")==false)
						isOver=false;
			}
			if(isOver) 
				break;
			
		}		
		
		String str="";
		String curWord="";
		try
		{
			for(int node=1;node<agendas.heap.array[0].nodes.size();node++){
					if(agendas.heap.array[0].nodes.get(node)==1)	continue;
					
					
					curWord=graph.nodes.get(agendas.heap.array[0].nodes.get(node)).wordString;
					str+=curWord+" ";
				}
				str+="#1_"+agendas.heap.array[0].score;
		}
		catch(Exception eee)
		{
			System.out.println(eee.getMessage());
			str=graph.sentences.get(0).toString();
			
			
			String[] senStr=str.split("\\s");
			str="";
			for(int kkkk=0;kkkk<senStr.length;kkkk++){
				boolean found=false;
				int count=0;
				if(graph.nodes.size()>52) count=Math.round(graph.nodes.size()*0.5f);
				else count=Math.round(graph.nodes.size()*0.7f);
				for(int k=0;k<graph.sortedNodes.size()&&k<count;k++){
					if(graph.sortedNodes.get(k).wordString.equalsIgnoreCase(senStr[kkkk]))
						found=true;
				}
				if(found) str+=senStr[kkkk]+" ";
			}
			str+="#1_"+"0";
			System.out.println("NULL:\t\t"+str);
		}
		
		
		String tmpStr=str.substring(str.lastIndexOf("#"));
		String[] titleWords=str.substring(0, str.lastIndexOf("#")).split("\\s");
		ArrayList<String> titlesList=new ArrayList<String>();
		String tmpWord="";
		
		for(int kkkk=0;kkkk<titleWords.length;kkkk++){
			if(titlesList.indexOf(titleWords[kkkk])<0){
				tmpWord = GetNounPhrase(titleWords[kkkk], titles.get(docCount).chains);			
				tmpWord = tmpWord.replace("_", " ");
				String[] tmpwords=tmpWord.split("\\s");
				for(int kk=tmpwords.length-2;kk>=0;kk--){
					if(titlesList.size()>0&&titlesList.get(titlesList.size()-1).equals(tmpwords[kk]))
						titlesList.remove(titlesList.size()-1);
					else
						break;
				}
				for(int kk=0;kk<tmpwords.length;kk++){
					titlesList.add(tmpwords[kk]);		
				}
			}
		}
		

		
		str="";
		for(int kkkk=0;kkkk<titlesList.size();kkkk++){
			if(titlesList.get(kkkk).indexOf("-")>-1) continue;
			str+=titlesList.get(kkkk)+" ";
		}
		str+=tmpStr;
		outFileWriter.write("Headline:"+str+"\n");
		System.out.println(str);
		
		outFileWriter.flush();
		return str;
	}
	
	public static boolean HasVerb(PartialResult newPartialResult){
		for(int i=0;i<newPartialResult.nodes.size();i++){
			if(graph.verbs.indexOf(graph.nodes.get(newPartialResult.nodes.get(i)).wordString)>-1)
				return true;
		}
		return false;
	}
	public static String GetNounPhrase(String noun,ArrayList<LexicalChain> chain){
		for(int i=0;i<chain.size();i++){
			for(int j=0;j<chain.get(i).lexicalList.size();j++){
				if(chain.get(i).lexicalList.get(j).getLexical().endsWith(noun)){
					return chain.get(i).lexicalList.get(j).getLexical();
				}
			}
		}
		return noun;
	}
	
	public static String GetMainWord(String noun,ArrayList<LexicalChain> chain){
		for(int i=0;i<chain.size();i++){
			for(int j=0;j<chain.get(i).lexicalList.size();j++){
				if(chain.get(i).lexicalList.get(j).getLexical().endsWith(noun)){
					return chain.get(i).lexicalList.get(j).mainWord;
				}
			}
		}
		return noun;
	}
	
	public static String OutputPartialResult(PartialResult a){
		String str="";
		String curWord="";
		for(int node=0;node<a.nodes.size();node++){
						
			curWord=graph.nodes.get(a.nodes.get(node)).wordString;
			str+=curWord+"_"+graph.nodes.get(a.nodes.get(node)).salience+" ";
		}
		str+=" : "+a.score+","+a.fluencyscore+","+a.pathscore;
		return str;
		
	}
	public static PartialResult DeepCopyPartialResult(PartialResult source){
		PartialResult target=new PartialResult();
		target.score=source.score;
		for(int i=1;i<source.nodes.size();i++){
			int k=source.nodes.get(i);
			target.nodes.add(k);
		}
		return target;
	}
	
	public static WordNode DeepCopyWordNode(WordNode source){
		WordNode target=new WordNode();
		target.wordString=source.wordString;
		target.salience=source.salience;		
		return target;
	}
	
	
	public static double CalPathScore(PartialResult newPartialResult) throws IOException{
		double score=0;
		double edgeWeight=0;
		int edgeCount=0;
		
		for(int node=0;node<newPartialResult.nodes.size()-1;node++){
			
			edgeCount++;			
			edgeWeight=graph.edgeWeights[newPartialResult.nodes.get(node)][newPartialResult.nodes.get(node+1)];
			score+=edgeWeight;
		}
		if(edgeCount>=1) return score/edgeCount;
		return score;
	}
	
	public static double CalFluencyScore(PartialResult newPartialResult) throws IOException{
		double score=0;
		String sentenceStr="";
		String curWord="";
		int wordcount=0;
		for(int node=0;node<newPartialResult.nodes.size();node++){
			

			if(graph.nodes.get(newPartialResult.nodes.get(node)).wordString.equals("STARTNODE"))
				sentenceStr="<s>"+" ";
			else if (graph.nodes.get(newPartialResult.nodes.get(node)).wordString.equals("ENDNODE"))
				sentenceStr+="</s>";
			else {
				wordcount++;
				if(nGramList.containsKey(graph.nodes.get(newPartialResult.nodes.get(node)).wordString)==false)
					sentenceStr+="<unk>"+" ";	
				else
					sentenceStr+=graph.nodes.get(newPartialResult.nodes.get(node)).wordString+" ";
			}
		}
		sentenceStr=sentenceStr.trim();
		score=CalculateFluency(sentenceStr);
		return score;
	}
	
	public static double CalCoverageScore(PartialResult newPartialResult,int docCount) throws Exception{
		double score=0;
		String str="";
		String curWord="";
		for(int node=1;node<newPartialResult.nodes.size();node++){
			if(newPartialResult.nodes.get(node)==1)
				continue;
			
			curWord=graph.nodes.get(newPartialResult.nodes.get(node)).wordString;
			str+=curWord+" ";
		}
		str=str.trim();
		
		score=CalculateCoverage(str,docCount);
		return score+0.0001;
	}
	
	public static double CalTotalScore(double fluenceScore,double pathScore, PartialResult a){
		double score=0;
		int wordCount=0;
		if(a.nodes.get(a.nodes.size()-1)==1) wordCount=2;
		else wordCount=1;
		score=(ALPHA*Math.log(fluenceScore)+Math.log(pathScore))/(a.nodes.size()-wordCount);
		return score;
	}
	
	public static boolean FindWordInTriGram(String trigram){
/*		String[] wordsStrings=trigram.split("\\s");
		for(int i=0;i<wordsStrings.length;i++){
			if(wordsList.containsKey(wordsStrings[i])){
				return true;
			}
		}
		return false;*/
		return true;
	}
	
	public static double IncrementFluencyScore(PartialResult newPartialResult){
			
			String sentenceStr="";
			int wordcount=0;
			for(int count=0,i=newPartialResult.nodes.size()-1;i>=0&&count<3;i--,count++){
				if(graph.nodes.get(newPartialResult.nodes.get(i)).wordString.equals("STARTNODE"))
					sentenceStr="<s>"+" "+sentenceStr;
				else if (graph.nodes.get(newPartialResult.nodes.get(i)).wordString.equals("ENDNODE"))
					sentenceStr+="</s>";
				else {
					wordcount++;
					if(nGramList.containsKey(graph.nodes.get(newPartialResult.nodes.get(i)).wordString)==false)
						sentenceStr="<unk>"+" "+sentenceStr;	
					else
						sentenceStr=graph.nodes.get(newPartialResult.nodes.get(i)).wordString+" "+sentenceStr;
				}
			}
			if(sentenceStr.indexOf("the and")>-1)
				System.out.println(sentenceStr.trim());
			float tmpScore=(float)Math.pow(10,ExtractNGramScore(sentenceStr.trim()));			
			return tmpScore/wordcount;
	}
	
	public static double IncrementPathScore(PartialResult newPartialResult) throws IOException{
		
		return 0.0001;
	}
	
	public static void CreateNgramModel(String filename) throws IOException, FileNotFoundException{
		wordsList.put("<unk>",1);
		wordsList.put("<s>",1);
		wordsList.put("</s>",1);
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "utf-8"));
		String buf = "";
		float probScore=0,backOffScore=0;
		String tmpTrigram="";
		String[] tt;
		while ((buf = br.readLine()) != null) {
			buf = buf.trim().toLowerCase();
			if(buf.length()==0)
				continue;
			if(buf.indexOf("\t")<0) continue;
			try{
				tt=buf.split("\t");
				probScore=Float.valueOf(tt[0]);
				tmpTrigram=tt[1];
				if(tt.length==3) backOffScore=Float.valueOf(tt[2]);
				else backOffScore=0;	
				nGramList.put(tmpTrigram, new NGramScore(probScore,backOffScore));
			}
			catch(Exception ee){
				continue;
			}
		}
		br.close();
		System.out.println("ngram size:"+nGramList.size());
	}

	public static double CalculateFluency(String sentence) throws IOException{
		double score=0;
		
		String[] wordsStrings=sentence.split("\\s");
		String w1,w2,w3;
		if(wordsStrings.length==2){
			w1=wordsStrings[0];
			w2=wordsStrings[1];
			float tmpScore=(float)Math.pow(10,ExtractNGramScore(w1+" "+w2));
			score=tmpScore;
		}
		else{
			for(int i=2;i<wordsStrings.length;i++){
				w1=wordsStrings[i-2];
				w2=wordsStrings[i-1];
				w3=wordsStrings[i];
				float tmpScore=(float)Math.pow(10,ExtractNGramScore(w1+" "+w2+" "+w3));
				score+=tmpScore;				
				
			}
		}
		return score/wordsStrings.length;
	}
	
	public static double ExtractNGramScore(String wordSequence){
		String[] words=wordSequence.split("\\s");
	
		if(words.length==3){
			if(nGramList.containsKey(wordSequence))
				return nGramList.get(wordSequence).prob;
			else if(nGramList.containsKey(words[0]+" "+words[1]))
				return nGramList.get(words[0]+" "+words[1]).backOffProb+ExtractNGramScore(words[1]+" "+words[2]);
			else
				return ExtractNGramScore(words[1]+" "+words[2]);
		}
		else if(words.length==2){
			if(nGramList.containsKey(words[0]+" "+words[1]))
				return nGramList.get(words[0]+" "+words[1]).prob;
			else
				return nGramList.get(words[0]).backOffProb+ExtractNGramScore(words[1]);
		}
		else{
			return nGramList.get(words[0]).prob;			
		}
	}
	
	public static double CalculateCoverage(String sentence,int docID) throws IOException, Exception{		
		
		String[] ssStrings=sentence.split("\\s+");
		double salientScore=0;
		String tmp="";
		int chainCount=0;
		for(int kk=0;kk<ssStrings.length;kk++){
			int chainIndex=FindLexicalInChain(titles.get(docID), ssStrings[kk]);			
			if(chainIndex>-1){
				chainCount++;	
				salientScore+=titles.get(docID).chains.get(chainIndex).salientScore;
			}
		}		
		return salientScore;
	}
	
	public static int FindLexicalInChain(NewsContent content,String lexicalStr){
		LexicalChain chain;
		
		for(int i=0;i<content.chains.size();i++){
			chain=content.chains.get(i);
			for(int j=0;j<chain.lexicalList.size();j++){		
				if(chain.lexicalList.get(j).wordList.indexOf(lexicalStr)>-1){										
					return i;
				}
			}
		}		
		return -1;
	}
	


	public static Object DeSerielizeObject(String fileName) throws Exception, IOException{
		ObjectInputStream in = new ObjectInputStream(new FileInputStream(fileName));
		Object obj = in.readObject();
		in.close();
		return obj;
	}
	

	public static void SerielizeObject(Object obj,String fileName) throws FileNotFoundException, IOException{
		ObjectOutputStream oo = null;
		oo = new ObjectOutputStream(new FileOutputStream(fileName));
		oo.writeObject(obj);		
		oo.close();
	}
}
class SortPartialResultByScore implements Comparator{
	 public int compare(Object o1, Object o2) {
		 PartialResult s1 = (PartialResult) o1;
		 PartialResult s2 = (PartialResult) o2;
		 return (new Double(s2.score)).compareTo(new Double(s1.score));	
		 }
}
