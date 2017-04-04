
public class Typecheck
{
	public static void main(String[] args)
	{
		//just getting started
		if(args.length > 0)
		{
			//args[0] is the file.. use parser to interact with it?

			//just checking if java compiles and stuff, basically at "hello world" stage right now.
			System.out.println(args[0]);
		}
		else
		{
			System.out.println("Error, no file to typecheck.");
		}
	}
}
