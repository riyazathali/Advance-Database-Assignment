
/****************************************************************************************
 * @file  Table.java
 *
 * @author   John Miller
 */

import java.io.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static java.lang.Boolean.*;
import static java.lang.System.out;

/****************************************************************************************
 * This class implements relational database tables (including attribute names, domains
 * and a list of tuples.  Five basic relational algebra operators are provided: project,
 * select, union, minus and join.  The insert data manipulation operator is also provided.
 * Missing are update and delete data manipulation operators.
 */
public class Table
       implements Serializable
{
    /** Relative path for storage directory
     */
    private static final String DIR = "store" + File.separator;

    /** Filename extension for database files
     */
    private static final String EXT = ".dbf";

    /** Counter for naming temporary tables.
     */
    private static int count = 0;

    /** Table name.
     */
    private final String name;

    /** Array of attribute names.
     */
    private final String [] attribute;

    /** Array of attribute domains: a domain may be
     *  integer types: Long, Integer, Short, Byte
     *  real types: Double, Float
     *  string types: Character, String
     */
    private final Class [] domain;

    /** Collection of tuples (data storage).
     */
    private final List <Comparable []> tuples;

    /** Primary key. 
     */
    private final String [] key;

    /** Index into tuples (maps key to tuple number).
     */
    private final Map <KeyType, Comparable []> index;

    public int getTableSize() {
        return tuples.size();
    }

    public List<Comparable[]> getTuples() {
        return tuples;
    }


    /** The supported map types.
     */
    private enum MapType { NO_MAP, TREE_MAP, LINHASH_MAP, BPTREE_MAP }

    /** The map type to be used for indices.  Change as needed.
     */
    private static final MapType mType = MapType.NO_MAP;

    /************************************************************************************
     * Make a map (index) given the MapType.
     */
    private static Map <KeyType, Comparable []> makeMap ()
    {
        switch (mType) {
        case TREE_MAP:    return new TreeMap<KeyType, Comparable[]>();
        case LINHASH_MAP: return null; //return new LinHashMap <> (KeyType.class, Comparable [].class); //Change to this once LinHashMap is implemented
        case BPTREE_MAP:  return null; //return new BpTreeMap <> (KeyType.class, Comparable [].class); //Change to this once BpTreeMap is implemented
        default:          return null;
        } // switch
    } // makeMap

    //-----------------------------------------------------------------------------------
    // Constructors
    //-----------------------------------------------------------------------------------

    /************************************************************************************
     * Construct an empty table from the meta-data specifications.
     *
     * @param _name       the name of the relation
     * @param _attribute  the string containing attributes names
     * @param _domain     the string containing attribute domains (data types)
     * @param _key        the primary key
     */  
    public Table (String _name, String [] _attribute, Class [] _domain, String [] _key)
    {
        name      = _name;
        attribute = _attribute;
        domain    = _domain;
        key       = _key;
        tuples    = new ArrayList<Comparable[]>();
        index     = makeMap ();

    } // primary constructor

    /************************************************************************************
     * Construct a table from the meta-data specifications and data in _tuples list.
     *
     * @param _name       the name of the relation
     * @param _attribute  the string containing attributes names
     * @param _domain     the string containing attribute domains (data types)
     * @param _key        the primary key
     * @param _tuples     the list of tuples containing the data
     */  
    public Table (String _name, String [] _attribute, Class [] _domain, String [] _key,
                  List <Comparable []> _tuples)
    {
        name      = _name;
        attribute = _attribute;
        domain    = _domain;
        key       = _key;
        tuples    = _tuples;
        index     = makeMap ();
    } // constructor

    /************************************************************************************
     * Construct an empty table from the raw string specifications.
     *
     * @param _name       the name of the relation
     * @param attributes  the string containing attributes names
     * @param domains     the string containing attribute domains (data types)
     * @param _key        the primary key
     */
    public Table (String _name, String attributes, String domains, String _key)
    {
        this (_name, attributes.split (" "), findClass (domains.split (" ")), _key.split(" "));

        out.println ("DDL> create table " + name + " (" + attributes + ")");
    } // constructor

    //----------------------------------------------------------------------------------
    // Public Methods
    //----------------------------------------------------------------------------------

    /************************************************************************************
     * Project the tuples onto a lower dimension by keeping only the given attributes.
     * Check whether the original key is included in the projection.
     *
     * #usage movie.project ("title year studioNo")
     *
     * @param attributes  the attributes to project onto
     * @return  a table of projected tuples
     */
    public Table project (String attributes)
    {
        out.println ("RA> " + name + ".project (" + attributes + ")");
        String [] attrs     = attributes.split (" ");
        Class []  colDomain = extractDom (match (attrs), domain);
        String [] newKey    = (Arrays.asList (attrs).containsAll (Arrays.asList (key))) ? key : attrs;

        List <Comparable []> rows = new ArrayList<Comparable[]>();

        //  T O   B E   I M P L E M E N T E D  
        // Project implementing 1 started
           for(int i = 0; i < tuples.size(); i++){
            Comparable [] array = new Comparable[attrs.length];
            for(int j = 0; j < attrs.length; j++){
                int attrPos = col(attrs[j]);
                array[j] = tuples.get(i)[attrPos];
            }
            rows.add(array);
        }
        for(int x = 0; x < rows.size(); x++){
            for(int y = x+1; y < rows.size()-1; y++){
                if(rows.get(x).equals(rows.get(y))){
                    rows.remove(y);
                    x -= 1;
                }
            }
        }

        // Project implementing 1 finished

        return new Table (name + count++, attrs, colDomain, newKey, rows);
    } // project

    /************************************************************************************
     * Select the tuples satisfying the given predicate (Boolean function).
     *
     * #usage movie.select (t -> t[movie.col("year")].equals (1977))
     *
     * @param predicate  the check condition for tuples
     * @return  a table with tuples satisfying the predicate
     */
    public Table select (PredicateMod <Comparable []> predicate)
    {
        out.println ("RA> " + name + ".select (" + predicate + ")");
		List<Comparable[]> rows = new ArrayList<Comparable[]>();
        for(Comparable[] tuple:tuples) {
        	if(predicate.test(tuple)) {
        		rows.add(tuple);
        	}
        }
        return new Table (name + count++, attribute, domain, key, rows);
    } // select

    /************************************************************************************
     * Select the tuples satisfying the given key predicate (key = value).  Use an index
     * (Map) to retrieve the tuple with the given key value.
     *
     * @param keyVal  the given key value
     * @return  a table with the tuple satisfying the key predicate
     */
    public Table select (KeyType keyVal)
    {
        out.println ("RA> " + name + ".select (" + keyVal + ")");
        int[] keyIndex = match(key);
        List <Comparable []> rows = new ArrayList<Comparable[]>();

        //  T O   B E   I M P L E M E N T E D 
        // Select implementing 2 started

//        for(int i = 0; i < tuples.size(); i++){
//            if(tuples.get(i).equals(index.get(keyVal))){
//                rows.add(tuples.get(i));
//            }
//        }

//
//                tuples.forEach(t -> {
//                    for (int i = 0; i < attribute.length; i++) {
//                        if (new KeyType(t[i].toString()).equals(keyVal)) {
//                            rows.add(t);
//                        }
//                    }
//                });


        for(Comparable[] tuple:tuples) {
            int i;
            for ( i=0;i<keyVal.getKey().length; i++){

                if ( !tuple[keyIndex[i]].equals( keyVal.getKey()[i] )){
                    break;
                }
            }

            if (i == keyVal.getKey().length) {
                rows.add(tuple);
            }
        }


     // Select implementing 2 finished

        return new Table (name + count++, attribute, domain, key, rows);
    } // select

    /************************************************************************************
     * Union this table and table2.  Check that the two tables are compatible.
     *
     * #usage movie.union (show)
     *
     * @param table2  the rhs table in the union operation
     * @return  a table representing the union
     */
    public boolean checkDuplicateTuple(Comparable[] tuple1, Comparable[] tuple2){
        for (int i=0;i<tuple1.length;i++){
            if (!tuple1[i].equals(tuple2[i]))
                return false;
        }
        return true;
    }
    public Table union (Table table2)
    {
        out.println ("RA> " + name + ".union (" + table2.name + ")");
        if (! compatible (table2)) return null;

        List <Comparable []> rows = new ArrayList<Comparable[]>();

        //  T O   B E   I M P L E M E N T E D 

        // Union implementing 3 started

        if (tuples.size() > 0) {
            rows.add(tuples.get(0));
            int i;

            for (Comparable[] tuple : tuples) {
                for (i=0;i<rows.size();i++){
                    if (checkDuplicateTuple(tuple, rows.get(i)) ) {
                        break;
                    }
                }
                if (i==rows.size()){
                    rows.add(tuple);
                }
            }

            for (Comparable[] tuple : table2.tuples) {
                for (i=0;i<rows.size();i++){
                    if (checkDuplicateTuple(tuple, rows.get(i)) ) {
                        break;
                    }
                }
                if (i==rows.size()){
                    rows.add(tuple);
                }
            }
        }
         // Union implementing 3 finished


        return new Table (name + count++, attribute, domain, key, rows);
    } // union


    /************************************************************************************
     * Take the difference of this table and table2.  Check that the two tables are
     * compatible.
     *
     * #usage movie.minus (show)
     *
     * @param table2  The rhs table in the minus operation
     * @return  a table representing the difference
     */
    public Table minus (Table table2)
    {
        out.println ("RA> " + name + ".minus (" + table2.name + ")");
        if (! compatible (table2)) return null;

        List <Comparable []> rows = new ArrayList<Comparable[]>();

        //  T O   B E   I M P L E M E N T E D 

        // Minus implementing 4 started

        boolean RowisCopied=true;

        for( int i=0; i<tuples.size() ; i++) {
            //check if tuple is in table 2
            for (int j = 0; j < table2.tuples.size(); j++) {
                if (tuples.get(i)==table2.tuples.get(j)) {RowisCopied=false;}
            }
            //adding row to the new table row if the tuple exists in both tables
            if(RowisCopied){ rows.add(tuples.get(i)); }
            else{ RowisCopied=true; }
        }

        // Minus implementing 4 finished

        return new Table (name + count++, attribute, domain, key, rows);
    } // minus

    /************************************************************************************
     * Join this table and table2 by performing an "equi-join".  Tuples from both tables
     * are compared requiring attributes1 to equal attributes2.  Disambiguate attribute
     * names by append "2" to the end of any duplicate attribute name.  Implement using
     * a Nested Loop Join algorithm.
     *
     * #usage movie.join ("studioNo", "name", studio)
     *
     * @param table2      the rhs table in the join operation
     * @return  a table with tuples satisfying the equality predicate
     */
    public Table join (String attributes1, String attributes2, Table table2)
    {
        out.println ("RA> " + name + ".join (" + attributes1 + ", " + attributes2 + ", "
                                               + table2.name + ")");

        String [] t_attrs = attributes1.split (" ");
        String [] u_attrs = attributes2.split (" ");

        List <Comparable []> rows = new ArrayList<Comparable[]>();

        //  T O   B E   I M P L E M E N T E D 
        // Join implementing 5 started

        int [] t_attrsPos = new int [t_attrs.length];
        int [] u_attrsPos = new int [u_attrs.length];

        //Key's column position is getting caught from each table.
        for(int i = 0; i < t_attrs.length; i++) { t_attrsPos[i] = col(t_attrs[i]); }

        for(int i = 0; i < u_attrs.length; i++) { u_attrsPos[i] = table2.col(u_attrs[i]); }

        
        
        //checking the domains
        for(int i = 0; i < t_attrs.length; i++) {
            String d1 = domain[t_attrsPos[i]].getName();
            String d2 = table2.domain[u_attrsPos[i]].getName();
            if ( ! d1.equals(d2)) {
                out.println("The domain of attribute " + attribute[t_attrsPos[i]] + " is " + d1);
                out.println("The domain of attribute " + table2.attribute[u_attrsPos[i]] + " is " + d2);
                out.println("These domain dont match!");
                return null;
            }
        }        

        //New tuples creation
        for (Comparable[] tup1 : tuples) {
            for(Comparable[] tup2 : table2.tuples) {

                //int Data type is here used to assure that the each column is matched
                int match = 0;

                for(int k = 0; k < t_attrs.length; k++) {
                    if( tup1[t_attrsPos[k]] == tup2[u_attrsPos[k]]) {match++;}
                }

                if(match == t_attrs.length) {
                    //Creation of new tuple
                    Comparable[] row = ArrayUtil.concat(tup1,tup2);
                    rows.add(row);
                }
                  
            }
        }
        
        String[] newAttr = table2.attribute;
        for(int i = 0; i < attribute.length; i++) {
            for(int j = 0; j < table2.attribute.length; j++) {
                if(attribute[i].equals(table2.attribute[j])){
                    newAttr[j] = table2.attribute[j] + "2";
                }
            }
            
        }
    // Join implementing 5 finished

        return new Table (name + count++, ArrayUtil.concat (attribute, table2.attribute),
                                          ArrayUtil.concat (domain, table2.domain), key, rows);
    } // join

    /************************************************************************************
     * Join this table and table2 by performing an "equi-join".  Same as above, but implemented
     * using an Index Join algorithm.
     *
     * @param table2      the rhs table in the join operation
     * @return  a table with tuples satisfying the equality predicate
     */
    public Table i_join (String attributes1, String attributes2, Table table2)
    {
        return null;
    } // i_join

    /************************************************************************************
     * Join this table and table2 by performing an "equi-join".  Same as above, but implemented
     * using a Hash Join algorithm.
     *
     * @param table2      the rhs table in the join operation
     * @return  a table with tuples satisfying the equality predicate
     */
    public Table h_join (String attributes1, String attributes2, Table table2)
    {
        return null;
    } // h_join

    /************************************************************************************
     * Join this table and table2 by performing an "natural join".  Tuples from both tables
     * are compared requiring common attributes to be equal.  The duplicate column is also
     * eliminated.
     *
     * #usage movieStar.join (starsIn)
     *
     * @param table2  the rhs table in the join operation
     * @return  a table with tuples satisfying the equality predicate
     */
    public Table join (Table table2)
    {
        out.println ("RA> " + name + ".join (" + table2.name + ")");

        List <Comparable []> rows = new ArrayList<Comparable[]>();

        //  T O   B E   I M P L E M E N T E D 

        // join natural implementing 6 started

        
                int numDupCol = 0;
        for(int i = 0; i< table2.attribute.length; i++) {
            if( ! (col(table2.attribute[i]) == -1)) {
                numDupCol++;
            }
            
        }

        //storage for the column positions of duplicate
        int [] t_keysPos = new int [numDupCol];
        int [] u_keysPos = new int [numDupCol];


        //Key's column position is getting caught from each table.
        for (int i = 0; i > table2.attribute.length; i++) { t_keysPos[i] = col(table2.attribute[i]); }
        for(int i = 0; i < attribute.length; i++) { u_keysPos[i] = table2.col(attribute[i]); }
        
        //Checking for some special case
        boolean intersection = false;
        if(table2.attribute.length==attribute.length){
            if(numDupCol == attribute.length){
                intersection = true;
            }
        }
        //Pulling non-common attributes and the domains
        String[] newAttr = new String[table2.attribute.length - numDupCol];
        Class[] newDomain = new Class[table2.attribute.length - numDupCol];
        for(int i = 0; i<newAttr.length; i++){
            for(int j = 0; j <attribute.length; j++) {
                for(int k = 0; k < table2.attribute.length; k++){
                    if(!(attribute[j].equals(table2.attribute[k]))){
                        newAttr[i] = table2.attribute[k];
                        newDomain[i] = table2.domain[k];
                    }
                }
            }
        }

        if(numDupCol == 0) {
            //CrossProduct
            for (Comparable[] tup1 : tuples) {
                for (Comparable[] tup2 : table2.tuples){
                    Comparable[] newTup2 = extract(tup2, newAttr);
                    rows.add(ArrayUtil.concat(tup1,newTup2));
                }

                return new Table (name + count++, ArrayUtil.concat (attribute, table2.attribute),
                                          ArrayUtil.concat (domain, table2.domain), key, rows);
            }
        } else if(! intersection) {
            //Checking the domains
            
            for(int i = 0; i < t_keysPos.length; i++) {
                String d1 = domain[t_keysPos[i]].getName();
                String d2 = table2.domain[u_keysPos[i]].getName();
                if ( ! d1.equals(d2)) {
                    out.println("The domain of attribute " + attribute[t_keysPos[i]] + " is " + d1);
                    out.println("The domain of attribute " + table2.attribute[u_keysPos[i]] + " is " + d2);
                    out.println("These domain dont match!");
                    return null;
                }
            }

                //Creation of new tuples
            for (Comparable[] tup1 : tuples) {
                for(Comparable[] tup2 : table2.tuples) {

                    //int is ok to be used to make sure that the each column is matched
                    int match = 0;

                    for(int k = 0; k < t_keysPos.length; k++) {
                        if( tup1[t_keysPos[k]] == tup2[u_keysPos[k]]) {match++;}
                    }

                    if(match == t_keysPos.length) {

                        rows.add(ArrayUtil.concat(tup1, extract(tup2, newAttr)));
                    }
                    
                }
            }

            return new Table (name + count++, ArrayUtil.concat (attribute, newAttr),
                                          ArrayUtil.concat (domain, newDomain), key, rows);



        } else if(intersection) {

            //Intersection
            for (Comparable[] tup1 : tuples) {
                
                for(Comparable[] tup2: table2.tuples){
                    Comparable[] testTup = extract(tup2, attribute);
                    
                    boolean addRow = true;
                    

                    for(int i = 0; i < attribute.length; i++) {
                        if(!(testTup[i] == tup1[i])) {
                            addRow = false;
                        }
                    }
                    if(addRow){rows.add(testTup);}
                    
                }
            }

            return new Table (name + count++, attribute, domain, key, rows);

        }

        // join natural implementing 6 finished
        // FIX - eliminate duplicate columns
        return new Table (name + count++, ArrayUtil.concat (attribute, table2.attribute),
                                          ArrayUtil.concat (domain, table2.domain), key, rows);
    } // join

    /************************************************************************************
     * Return the column position for the given attribute name.
     *
     * @param attr  the given attribute name
     * @return  a column position
     */
    public int col (String attr)
    {
        for (int i = 0; i < attribute.length; i++) {
           if (attr.equals (attribute [i])) return i;
        } // for

        return -1;  // not found
    } // col

    /************************************************************************************
     * Insert a tuple to the table.
     *
     * #usage movie.insert ("'Star_Wars'", 1977, 124, "T", "Fox", 12345)
     *
     * @param tup  the array of attribute values forming the tuple
     * @return  whether insertion was successful
     */
    public boolean insert (Comparable [] tup)
    {
        out.println ("DML> insert into " + name + " values ( " + Arrays.toString (tup) + " )");

        if (typeCheck (tup)) {
            tuples.add (tup);
            Comparable [] keyVal = new Comparable [key.length];
            int []        cols   = match (key);
            for (int j = 0; j < keyVal.length; j++) keyVal [j] = tup [cols [j]];
            if (mType != MapType.NO_MAP) index.put (new KeyType (keyVal), tup);
            return true;
        } else {
            return false;
        } // if
    } // insert

    /************************************************************************************
     * Get the name of the table.
     *
     * @return  the table's name
     */
    public String getName ()
    {
        return name;
    } // getName

    /************************************************************************************
     * Print this table.
     */
    public void print ()
    {
        out.println ("\n Table " + name);
        out.print ("|-");
        for (int i = 0; i < attribute.length; i++) out.print ("---------------");
        out.println ("-|");
        out.print ("| ");
        for (String a : attribute) out.printf ("%15s", a);
        out.println (" |");
        out.print ("|-");
        for (int i = 0; i < attribute.length; i++) out.print ("---------------");
        out.println ("-|");
        for (Comparable [] tup : tuples) {
            out.print ("| ");
            for (Comparable attr : tup) out.printf ("%15s", attr);
            out.println (" |");
        } // for
        out.print ("|-");
        for (int i = 0; i < attribute.length; i++) out.print ("---------------");
        out.println ("-|");
    } // print

    /************************************************************************************
     * Print this table's index (Map).
     */
    public void printIndex ()
    {
        out.println ("\n Index for " + name);
        out.println ("-------------------");
        if (mType != MapType.NO_MAP) {
            for (Map.Entry <KeyType, Comparable []> e : index.entrySet ()) {
                out.println (e.getKey () + " -> " + Arrays.toString (e.getValue ()));
            } // for
        } // if
        out.println ("-------------------");
    } // printIndex

    /************************************************************************************
     * Load the table with the given name into memory. 
     *
     * @param name  the name of the table to load
     */
    public static Table load (String name)
    {
        Table tab = null;
        try {
            ObjectInputStream ois = new ObjectInputStream (new FileInputStream (DIR + name + EXT));
            tab = (Table) ois.readObject ();
            ois.close ();
        } catch (IOException ex) {
            out.println ("load: IO Exception");
            ex.printStackTrace ();
        } catch (ClassNotFoundException ex) {
            out.println ("load: Class Not Found Exception");
            ex.printStackTrace ();
        } // try
        return tab;
    } // load

    /************************************************************************************
     * Save this table in a file.
     */
    public void save ()
    {
        try {
            ObjectOutputStream oos = new ObjectOutputStream (new FileOutputStream (DIR + name + EXT));
            oos.writeObject (this);
            oos.close ();
        } catch (IOException ex) {
            out.println ("save: IO Exception");
            ex.printStackTrace ();
        } // try
    } // save

    //----------------------------------------------------------------------------------
    // Private Methods
    //----------------------------------------------------------------------------------

    /************************************************************************************
     * Determine whether the two tables (this and table2) are compatible, i.e., have
     * the same number of attributes each with the same corresponding domain.
     *
     * @param table2  the rhs table
     * @return  whether the two tables are compatible
     */
    private boolean compatible (Table table2)
    {
        if (domain.length != table2.domain.length) {
            out.println ("compatible ERROR: table have different arity");
            return false;
        } // if
        for (int j = 0; j < domain.length; j++) {
            if (domain [j] != table2.domain [j]) {
                out.println ("compatible ERROR: tables disagree on domain " + j);
                return false;
            } // if
        } // for
        return true;
    } // compatible

    /************************************************************************************
     * Match the column and attribute names to determine the domains.
     *
     * @param column  the array of column names
     * @return  an array of column index positions
     */
    private int [] match (String [] column)
    {
        int [] colPos = new int [column.length];

        for (int j = 0; j < column.length; j++) {
            boolean matched = false;
            for (int k = 0; k < attribute.length; k++) {
                if (column [j].equals (attribute [k])) {
                    matched = true;
                    colPos [j] = k;
                } // for
            } // for
            if ( ! matched) {
                out.println ("match: domain not found for " + column [j]);
            } // if
        } // for

        return colPos;
    } // match

    /************************************************************************************
     * Extract the attributes specified by the column array from tuple t.
     *
     * @param t       the tuple to extract from
     * @param column  the array of column names
     * @return  a smaller tuple extracted from tuple t 
     */
    private Comparable [] extract (Comparable [] t, String [] column)
    {
        Comparable [] tup = new Comparable [column.length];
        int [] colPos = match (column);
        for (int j = 0; j < column.length; j++) tup [j] = t [colPos [j]];
        return tup;
    } // extract

    /************************************************************************************
     * Check the size of the tuple (number of elements in list) as well as the type of
     * each value to ensure it is from the right domain. 
     *
     * @param t  the tuple as a list of attribute values
     * @return  whether the tuple has the right size and values that comply
     *          with the given domains
     */
    private boolean typeCheck (Comparable [] t)
    { 
        //  T O   B E   I M P L E M E N T E D 

        // Insert implementing 7 started

        if( ! (domain.length == t.length)) return false;

        
        for(int i = 0; i < t.length; i++) {

            if( ! (domain[i].isInstance(t[i]) )) { 
                
                //checking for case whether the attribute is a double or not but is supposed to be a float Data type.
                if( t[i] instanceof Double){
                    if(domain[i].getName().equals("Float")) {
                        return true;
                    }

                } else {
                    out.println("Incorrect entry for the given domain for " + attribute[i]);
                    return false;
                }
                
            } 
            
        }

         // Insert implementing 7 finished

        return true;
    } // typeCheck

    /************************************************************************************
     * Find the classes in the "java.lang" package with given names.
     *
     * @param className  the array of class name (e.g., {"Integer", "String"})
     * @return  an array of Java classes
     */
    private static Class [] findClass (String [] className)
    {
        Class [] classArray = new Class [className.length];

        for (int i = 0; i < className.length; i++) {
            try {
                classArray [i] = Class.forName ("java.lang." + className [i]);
            } catch (ClassNotFoundException ex) {
                out.println ("findClass: " + ex);
            } // try
        } // for

        return classArray;
    } // findClass

    /************************************************************************************
     * Extract the corresponding domains.
     *
     * @param colPos the column positions to extract.
     * @param group  where to extract from
     * @return  the extracted domains
     */
    private Class [] extractDom (int [] colPos, Class [] group)
    {
        Class [] obj = new Class [colPos.length];

        for (int j = 0; j < colPos.length; j++) {
            obj [j] = group [colPos [j]];
        } // for

        return obj;
    } // extractDom

} // Table class

