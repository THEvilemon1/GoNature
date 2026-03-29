import java.util.ArrayList;

/**
 * This class represents shared data used between threads or components.
 * It stores a list of integers, a boolean array indicating winners,
 * a flag, and a constant value b.
 * 
 * @author kablanyareen-hub
 * @version 1.0
 */
public class SharedData 
{
	private ArrayList<Integer> array;
	private boolean[] winArray;
	private boolean flag;
	private final int b;
	
    /**
     * Constructor that initializes the shared data.
     * 
     * @param array input array of integers (used to initialize internal list)
     * @param b constant value
     */
    public SharedData(ArrayList<Integer> array, int b) {
	      this.array = array;
		  this.b = b;
	}

    /**
     * Returns the win array.
     * 
     * @return boolean array representing winners
     */
	public boolean[] getWinArray() 
	{
		return winArray;
	}

    /**
     * Sets the win array.
     * 
     * @param winArray boolean array representing winners
     */
	public void setWinArray(boolean [] winArray) 
	{
		this.winArray = winArray;
	}

    /**
     * Returns the list of integers.
     * 
     * @return ArrayList of integers
     */
	public ArrayList<Integer> getArray() 
	{
		return array;
	}

    /**
     * Returns the constant value b.
     * 
     * @return value of b
     */
	public int getB() 
	{
		return b;
	}

    /**
     * Returns the flag value.
     * 
     * @return flag value
     */
	public boolean getFlag() 
	{
		return flag;
	}

    /**
     * Sets the flag value.
     * 
     * @param flag boolean value to set
     */
	public void setFlag(boolean flag) {
		this.flag = flag;
	}

}