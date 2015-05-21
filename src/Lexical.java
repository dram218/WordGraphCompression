import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;


public class Lexical  implements Serializable{
	ArrayList<String> wordList;
	int lineNo;
	ArrayList<String> wordNoList;
	boolean isAdded=false;
	String mainWord;
	String lexString;
	String lemmaWord;
	public Lexical(){wordList=new ArrayList<String>();}
	public Lexical(String lexical,int line){
		wordList=new ArrayList<String>();
		wordNoList=new ArrayList<String>();
		
		lineNo=line;
		lexString=lexical;
		String[] sent;
		int index=0;
		String word="";
		String wordno=""; 
		sent=lexical.split(",");
		for(int i=0;i<sent.length;i++){
			index=sent[i].lastIndexOf("-");
			if(index==-1){continue;}
			word = sent[i].substring(0,index);
			wordno=sent[i].substring(index+1);
			wordList.add(word);
			wordNoList.add(wordno);
		}
		mainWord=wordList.get(wordList.size()-1);
		lemmaWord=mainWord;
	}
	public int getLineNo(){
		return this.lineNo;
	}
	public String getLexical(){
		String leString="";
		for(int i=0;i<wordList.size()-1;i++){
			leString+=wordList.get(i)+"_";
		}
		leString+=wordList.get(wordList.size()-1);
		return leString;
	}
	public String getWordNoList(){
		String leString="";
		int i=0;
		for(;i<wordNoList.size()-1;i++){
			leString+=wordNoList.get(i)+",";
		}
		leString+=wordNoList.get(i);
		return leString;
	}
	public String getAllInfo(){
		return getLexical()+"-"+getWordNoList()+"_"+String.valueOf(lineNo);
	}
	public void setAdded(){
		this.isAdded=true;
	}
	private void writeObject(ObjectOutputStream stream) throws IOException {

		stream.defaultWriteObject();

		stream.writeObject(wordList);
		stream.writeObject(lineNo);
		stream.writeObject(wordNoList);
		stream.writeObject(isAdded);
		stream.writeObject(mainWord);
		stream.writeObject(lexString);
		stream.writeObject(lemmaWord);
	}

	private void readObject(ObjectInputStream stream) throws IOException,ClassNotFoundException {

		stream.defaultReadObject();
		wordList = (ArrayList<String> )stream.readObject();
		lineNo = (Integer)stream.readObject();
		wordNoList = (ArrayList<String>)stream.readObject();
		isAdded = (boolean)stream.readObject();
		mainWord = (String)stream.readObject();	
		lexString = (String)stream.readObject();	
		lemmaWord = (String)stream.readObject();	

	}
}
