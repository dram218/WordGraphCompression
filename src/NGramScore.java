import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;


public class NGramScore implements Serializable{
	float prob;
	float backOffProb;
	public NGramScore(float _prob,float _backOffProb){
		prob=_prob;
		backOffProb=_backOffProb;
	}

	private void writeObject(ObjectOutputStream stream) throws IOException {

		stream.defaultWriteObject();

		stream.writeObject(prob);
		stream.writeObject(backOffProb);
	}

	private void readObject(ObjectInputStream stream) throws IOException,ClassNotFoundException {

		stream.defaultReadObject();
		prob = (float)stream.readObject();
		backOffProb = (float)stream.readObject();

	}
}
