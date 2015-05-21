import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;


public class NewsContent implements Serializable{
	ArrayList<LexicalChain> chains;
	int wordCount=0;
	int lineCount=0;
	public NewsContent(){
		chains=new ArrayList<LexicalChain>();
	}
	
	private void writeObject(ObjectOutputStream stream) throws IOException {

		stream.defaultWriteObject();

		stream.writeObject(chains);
		stream.writeObject(wordCount);
		stream.writeObject(lineCount);
	}

	private void readObject(ObjectInputStream stream) throws IOException,ClassNotFoundException {

		stream.defaultReadObject();
		chains = (ArrayList<LexicalChain>)stream.readObject();
		wordCount = (Integer)stream.readObject();
		lineCount = (Integer)stream.readObject();
	}

}
