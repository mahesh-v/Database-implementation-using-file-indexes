********************************************************
                CS6360: Database Design
		          Files and Indexing
********************************************************

Database schema supported as given in .csv file.

To run,
Option 1:
The dist folder contains the jar file.
This can be executed as "java -jar MyDatabase.jar"

Option 2:
The source code can be compiled using the build file provided.
Install ANT - http://ant.apache.org/ and follow instructions
in http://ant.apache.org/manual/index.html to set up.
Go to the folder which contains the build file, and the src folder.
Execute command ant;
The jar file is built and appears in the dist folder.
Execute jar as in Option 1.


USAGE:

1. Import:
prompt> import file_name.csv;
prompt> import file_name.csv as pharma;

Note that in the above example, if no alias is provided, the 
database table will be called "file_name".
Else, it will take the alias name, "pharma".

2. Query:
prompt> SELECT [comma_separated_field_list | *]
  FROM PHARMA_TRIALS_1000B
  WHERE field_name [!=|=|>|>=|<|<=] value;
  
3. Insert:
prompt> INSERT INTO table_name VALUES (values_list);

Note that all fields in the comma separated values_list are mandatory.

4. Delete:
prompt> DELETE FROM table_name WHERE field_name [!=|=|>|>=|<|<=] value;

Note that the WHERE clause is mandatory for delete to prevent accidental
deletion of the entire table.

5. Quit:
prompt> quit;
prompt> exit;

