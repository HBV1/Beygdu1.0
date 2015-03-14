package is.arnastofnun.DB;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;

import is.arnastofnun.parser.Block;
import is.arnastofnun.parser.SubBlock;
import is.arnastofnun.parser.Tables;
import is.arnastofnun.parser.WordResult;

/**
 * @author Jón Friðrik
 * @since 14.02.15
 * @version 0.1
 *
 */
public class DBController {

    private Context context = null;
    private SQLiteDatabase dB = null;
    private DBHelper dbHelper = null;

    String[] wordResultColumns = new String[] {DBHelper.WORDID, DBHelper.TYPE, DBHelper.TITLE, DBHelper.NOTE, DBHelper.NOTE };
    String[] blockColumns = new String[] {DBHelper.WORDID, DBHelper.BLOCKID, DBHelper.TITLE};
    String[] subBlockColumns = new String[] {DBHelper.BLOCKID, DBHelper.SUBBLOCKID, DBHelper.TITLE};
    String[] tableColumns = new String[] {DBHelper.SUBBLOCKID, DBHelper.TABLEID, DBHelper.TITLE, DBHelper.COLHEADERS, DBHelper.ROWHEADERS, DBHelper.CONTENT };


    public DBController(Context context){
        this.context = context;
    }

    private DBController open() throws SQLException {
        dbHelper = new DBHelper(context);
        dB = dbHelper.getWritableDatabase();
        return this;
    }

    private void close() {
        dbHelper.close();
    }

    public void insert(WordResult result) {
        if(!dbContains(result.getTitle())) {
            int rows = getWordResultSize();
            if(rows >=  DBHelper.MAX_SIZE)  {
                removeOldest();
            }
            insertWordResult(result);
        }
    }

    private int getWordResultSize() {
        try {
            open();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        final String myQuery =
                "SELECT COUNT(*) FROM wordresult";

        Cursor cursor = dB.rawQuery(myQuery, null);

        if (cursor != null) {
            cursor.moveToFirst();
        }
        close();
        return cursor.getInt(0);
    }

    private void insertWordResult(WordResult result) {
        try {
            open();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        //Generate Content for all tables
        ContentValues wordResultContent = new ContentValues();

        wordResultContent.put(DBHelper.TYPE, result.getSearchWord());
        wordResultContent.put(DBHelper.TYPE, result.getDescription());
        wordResultContent.put(DBHelper.TITLE, result.getTitle());
        wordResultContent.put(DBHelper.NOTE, result.getWarning());
        wordResultContent.put(DBHelper.DATE, new Date().getTime());
        dB.insert(DBHelper.TABLE_WORDRESULT, null, wordResultContent);

        int wordResultID = fetchMaxId(DBHelper.WORDID, DBHelper.TABLE_WORDRESULT);
        dbHelper.close();

        for(Block block : result.getBlocks()) {
            insertBlocks(block, wordResultID);
        }
        close();
    }

    private void insertBlocks(Block block, int wordResultId) {
        dB = dbHelper.getWritableDatabase();

        ContentValues blockContent = new ContentValues();
        blockContent.put(DBHelper.WORDID, wordResultId);
        blockContent.put(DBHelper.TITLE, block.getTitle());
        dB.insert(DBHelper.TABLE_BLOCK, null, blockContent);

        int blockid = fetchMaxId(DBHelper.BLOCKID, DBHelper.TABLE_BLOCK);
        dbHelper.close();
        for(SubBlock sb : block.getBlocks()) {
            insertSubBlock(sb, blockid);
        }
    }

    private void insertSubBlock(SubBlock sb, int blockID) {
        dB = dbHelper.getWritableDatabase();

        ContentValues subBlockContent = new ContentValues();
        subBlockContent.put(DBHelper.BLOCKID, blockID);
        subBlockContent.put(DBHelper.TITLE, sb.getTitle());
        dB.insert(DBHelper.TABLE_SUBBLOCK, null, subBlockContent);

        int subBlockID = fetchMaxId(DBHelper.SUBBLOCKID, DBHelper.TABLE_SUBBLOCK);
        dbHelper.close();

        for(Tables table: sb.getTables()){
            insertTable(table, subBlockID);
        }
    }

    private void insertTable(Tables table, int subBlockID) {
        dB = dbHelper.getWritableDatabase();

        ContentValues tableContent = new ContentValues();
        tableContent.put(DBHelper.SUBBLOCKID, subBlockID);
        tableContent.put(DBHelper.TITLE, table.getTitle());
        tableContent.put(DBHelper.COLHEADERS, arrToString(table.getColumnNames()));
        tableContent.put(DBHelper.ROWHEADERS, arrToString(table.getRowNames()));
        tableContent.put(DBHelper.CONTENT, arrToString(table.getContent().toArray()));
        dB.insert(DBHelper.TABLE_TABLES, null, tableContent);

        dbHelper.close();
    }

    /**
     *
     * @param wordTitle the title of the WordResult
     * @return true if db contains word, else false
     */
    private boolean dbContains(String wordTitle){
        boolean contains = false;

        try {
            open();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        final String myQuery =
                "SELECT * FROM wordresult " +
                "WHERE " + DBHelper.TITLE + " = '"+ wordTitle +"'";

        Cursor cursor = dB.rawQuery(myQuery, null);

        if (cursor != null) {
            cursor.moveToFirst();

            if(cursor.getCount() > 0){
                contains = true;
                updateDate(wordTitle, cursor);

                close();
                return contains;
            }
        }

        close();
        return contains;
    }

    private void updateDate(String wordTitle, Cursor cursor) {
        String type = cursor.getString(1);
        String title = cursor.getString(2);
        String note = cursor.getString(3);

        ContentValues contentValues = new ContentValues();
        contentValues.put(DBHelper.TYPE, type);
        contentValues.put(DBHelper.TITLE, title);
        contentValues.put(DBHelper.NOTE, note);
        contentValues.put(DBHelper.DATE, new Date().getTime());

        dB.update(DBHelper.TABLE_WORDRESULT, contentValues, DBHelper.TITLE + "='" + wordTitle+"'", null);
    }


    /**
     *
     * @param title the title to be fetched
     * @return the first occurance WordResult for the title in the table
     */
    public WordResult fetch(String title) {
        WordResult newWordResult;

        if (!dB.isOpen()) {
            try {
                open();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        final String myQuery =
                "SELECT * FROM wordresult " +
                "JOIN block ON wordresult.wordid = block.wordid " +
                "JOIN subblock ON block.blockid = subblock.blockid " +
                "JOIN tables ON subblock.subblockid = tables.subblockid " +
                "WHERE wordresult.title = '"+ title +"'";

        Cursor cursor = dB.rawQuery(myQuery, null);

        if (cursor != null) {
            cursor.moveToFirst();
        }
        newWordResult = new WordResult();
        newWordResult.setDescription(cursor.getString(1));
        newWordResult.setTitle(cursor.getString(2));
        newWordResult.setWarning(cursor.getString(3));
        //newWordResult = new WordResult(cursor.getString(1), cursor.getString(2), cursor.getString(3));
        newWordResult.setBlocks(fetchBlocks(cursor));


        updateDate(title, cursor);
        close();
        return newWordResult;
    }

    private ArrayList<Block> fetchBlocks(Cursor cursor) {
        ArrayList<Block> blocks = new ArrayList<Block>();
        int blockID = -1;
        for (int i = 0; i < cursor.getCount(); i++) {
            cursor.moveToPosition(i);
            if (blockID != cursor.getInt(6)) {
                blockID = cursor.getInt(6);
                blocks.add(new Block(cursor.getString(7), fetchSubBlocks(cursor, cursor.getInt(6))));
            }
        }
        return blocks;
    }

    private ArrayList<SubBlock> fetchSubBlocks(Cursor cursor, int blockID) {
        ArrayList<SubBlock> subBlocks = new ArrayList<SubBlock>();
        int subBlockID = -1;
        for (int i = 0; i < cursor.getCount(); i++) {
            cursor.moveToPosition(i);
            if(cursor.getInt(9) != subBlockID && cursor.getInt(8) == blockID){
                subBlockID = cursor.getInt(9);
                subBlocks.add(new SubBlock(cursor.getString(10), fetchTables(cursor, cursor.getInt(9))));
            }
        }
        return subBlocks;
    }
    
    private ArrayList<Tables> fetchTables(Cursor cursor, int subBlockID) {
        ArrayList<Tables> tables = new ArrayList<Tables>();
        int tableID = -1;
        for (int i = 0; i < cursor.getCount(); i++) {
            cursor.moveToPosition(i);
            if(cursor.getInt(12) != tableID && cursor.getInt(11) == subBlockID) {
                tableID = cursor.getInt(12);
                tables.add(new Tables(cursor.getString(13), stringToArr(cursor.getString(14)),
                        stringToArr(cursor.getString(15)), new ArrayList<String>(Arrays.asList(stringToArr(cursor.getString(16))))));
            }
        }
        return tables;
    }

    public ArrayList<String> fetchAllWords() {
        ArrayList<String> words= new ArrayList<String>();
        try {
            open();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        final String myQuery = "SELECT * FROM " +
                DBHelper.TABLE_WORDRESULT +
                " ORDER BY " + DBHelper.DATE + " DESC";

        Cursor cursor = dB.rawQuery(myQuery, null);

        int iTitle = cursor.getColumnIndex(DBHelper.TITLE);

        for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            String tmp = cursor.getString(iTitle);
            words.add(tmp);
        }

        close();
        return words;
    }

    /**
     *
     * @param word the title of the WordResult
     * @return the id of the word in the database.
     */
    private int fetchWordId(String word) {
        int id = 0;
        final String MY_QUERY = "SELECT " +DBHelper.WORDID +" FROM " + DBHelper.TABLE_WORDRESULT + " WHERE " + DBHelper.TITLE + " = word";
        Cursor mCursor = dB.rawQuery(MY_QUERY, null);
        try {
            if (mCursor.getCount() > 0) {
                mCursor.moveToFirst();
                id = mCursor.getInt(0);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return id;
    }

    private int fetchMaxId(String column, String table) {
        int id = 0;
        final String MY_QUERY = "SELECT MAX("+ column +") FROM " + table;
        Cursor mCursor = dB.rawQuery(MY_QUERY, null);
        try {
            if (mCursor.getCount() > 0) {
                mCursor.moveToFirst();
                id = mCursor.getInt(0);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return id;
    }

    private int fetchMinId(String column, String table) {
        int id = 0;
        final String MY_QUERY = "SELECT MIN("+ column +") FROM " + table;
        Cursor mCursor = dB.rawQuery(MY_QUERY, null);
        try {
            if (mCursor.getCount() > 0) {
                mCursor.moveToFirst();
                id = mCursor.getInt(0);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return id;
    }

    private void removeOldest(){
        try {
            open();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        int wordID = fetchMinId(DBHelper.WORDID, DBHelper.TABLE_WORDRESULT);
        dB.delete(DBHelper.TABLE_WORDRESULT, DBHelper.WORDID + "=" + wordID, null);
        close();
    }

    private String arrToString(Object[] arr){
        String result = "";
        for (int i = 0; i < arr.length; i++) {
            result = result + "&" + arr[i];
        }
        return result;
    }

    private String[] stringToArr(String s) {
        if (s.startsWith("&")) {
            s = s.substring(1, s.length());
        }
        String[] arr = s.split("&+");
        return arr;
    }
}
