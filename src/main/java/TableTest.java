import static java.lang.System.out;
import java.util.*;

import static org.junit.Assert.*;

import org.junit.Test;

public class TableTest
{
	/**
	 * Generates a movie table for testing
	 * 
	 * @return a sample movie table
	 */
	public Table createMovieTable()
	{
		Table movie = new Table ("movie", "title year length genre studioName producerNo",
                "String Integer Integer String String Integer", "title year");
		Comparable [] film0 = { "Star_Wars", 1977, 124, "sciFi", "Fox", 12345 };
        Comparable [] film1 = { "Star_Wars_2", 1980, 124, "sciFi", "Fox", 12345 };
        Comparable [] film2 = { "Rocky", 1985, 200, "action", "Universal", 12125 };
        Comparable [] film3 = { "Rambo", 1978, 100, "action", "Universal", 32355 };
        movie.insert (film0);
        movie.insert (film1);
        movie.insert (film2);
        movie.insert (film3);
        return movie;
	}
	
	/**
	 * Generates a cinema table for testing
	 * 
	 * @return a sample movie table
	 */
	public Table createCinemaTable()
	{
		Table cinema = new Table ("movie", "title year length genre studioName producerNo",
                "String Integer Integer String String Integer", "title year");
		Comparable [] film0 = { "Rocky", 1985, 200, "action", "Universal", 12125 };
        Comparable [] film1 = { "Rambo", 1978, 100, "action", "Universal", 32355 };
		Comparable [] film2 = { "Galaxy_Quest", 1999, 104, "comedy", "DreamWorks", 67890 };
        cinema.insert (film0);
        cinema.insert (film1);
        cinema.insert (film2);
        return cinema;
	}
	
	/**
	 * Tests the project method.
	 */
	@Test
	public void testProject()
	{
		Table movie = this.createMovieTable();
		Table movie_project = movie.project ("title year");
		
		assertEquals(movie_project.col("title"), 0);
		assertEquals(movie_project.col("year"), 1);
		assertEquals(movie_project.col("length"), -1);
		assertEquals(movie_project.getTuples().get(0)[0], "Star_Wars");
		assertEquals(movie_project.getTuples().get(0).length, 2);
	}
	
	/**
	 * Tests the select method.
	 */
	@Test
	public void testSelect()
	{
		Table movie = this.createMovieTable();
		Table movie_select = movie.select(new KeyType("Star_Wars"));
		assertEquals(movie_select.getTableSize(), 1);
		List <Comparable[]> starWars = movie_select.getTuples();
		assertEquals(starWars.get(0)[0], "Star_Wars");
	}
	
	@Test
	public void testSelectPredicate()
	{
		 final Table movie = this.createMovieTable();
		PredicateMod<Comparable[]> predicate = new PredicateMod<Comparable[]>() {
			@Override
			public boolean test(Comparable[] t) {
				if ((Integer) t[movie.col("year")] < 1980) {
					return true;
				}
				return false;
			}
		};
		Table movie_select = movie.select(predicate);
		assertEquals(movie_select.getTableSize(), 2);
		List <Comparable[]> movies = movie_select.getTuples();
		assertEquals(movies.get(0)[0], "Star_Wars");
	}

	/**
	 * Tests the union method.
	 */
	@Test
	public void testUnion()
	{
		Table movie = this.createMovieTable();
		Table cinema = this.createCinemaTable();
		Table movie_union = movie.union (cinema);
		assertEquals(movie_union.getTableSize(), 5);
		List <Comparable[]> movies = movie_union.getTuples();
		assertEquals(movies.get(0)[0], "Star_Wars");
	}
}
