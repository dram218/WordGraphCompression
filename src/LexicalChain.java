import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;


public class LexicalChain  implements Serializable{
	ArrayList<Lexical> lexicalList;
	float weight;
	float salientScore;
	String HeadWord="";
	public LexicalChain(){
		lexicalList=new ArrayList<Lexical>();
	}
	
	public void SetWeigth(float w){
		weight=w;
	}
	
	public String GetAllInfo(){
		String info="";
		for(int i=0;i<lexicalList.size();i++){
			info+=lexicalList.get(i).getAllInfo()+" ";
		}
		return info.trim();
	}
	
	private void writeObject(ObjectOutputStream stream) throws IOException {

		stream.defaultWriteObject();

		stream.writeObject(lexicalList);
		stream.writeObject(weight);
		stream.writeObject(salientScore);
		stream.writeObject(HeadWord);
	}

	private void readObject(ObjectInputStream stream) throws IOException,ClassNotFoundException {

		stream.defaultReadObject();
		lexicalList = (ArrayList<Lexical>)stream.readObject();
		weight = (float)stream.readObject();
		salientScore = (float)stream.readObject();
		HeadWord=(String)stream.readObject();
	}

}
