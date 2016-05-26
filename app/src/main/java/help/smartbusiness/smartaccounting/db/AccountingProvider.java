package help.smartbusiness.smartaccounting.db;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.Nullable;

/**
 * Created by gamerboy on 19/5/16.
 */
public class AccountingProvider extends ContentProvider {

    public static final String TAG = "CustomerProvider";

    private AccountingDbHelper mDbHelper;

    private static final String AUTHORITY = AccountingProvider.class.getPackage().getName();

    public static final int CUSTOMERS = 100;
    public static final int CUSTOMERS_ID = 101;

    public static final int CUSTOMER_PURCHASES = 102;
    public static final int CUSTOMER_ID_PURCHASES = 103;

    public static final int CUSTOMER_CREDITS = 104;
    public static final int CUSTOMER_ID_CREDITS = 105;

    public static final int CUSTOMER_TRANSACTIONS = 106;
    public static final int CUSTOMER_ID_TRANSACTIONS = 107;

    public static final int CUSTOMER_PURCHASE_PURCHASE_ITEMS = 108;
    public static final int CUSTOMER_ID_PURCHASE_ID_PURCHASE_ITEMS = 109;

    private static final String CUSTOMERS_BASE_PATH = "customers";
    private static final String PURCHASES_BASE_PATH = "purchases";
    private static final String CREDITS_BASE_PATH = "credits";
    private static final String TRANSACTION_BASE_PATH = "transactions";
    private static final String PURCHASE_ITEMS_BASE_PATH = "purchase_items";

    public static final String CUSTOMER_CONTENT_URI = "content://"
            + AUTHORITY + "/" + CUSTOMERS_BASE_PATH;

    public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
            + "/vnd." + AccountingProvider.class.getPackage().getName()
            + CUSTOMERS_BASE_PATH;
    public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
            + "/vnd." + AccountingProvider.class.getPackage().getName()
            + CUSTOMERS_BASE_PATH;

    private static final UriMatcher mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        mUriMatcher.addURI(AUTHORITY, "customers", CUSTOMERS);
        mUriMatcher.addURI(AUTHORITY, "customers/#", CUSTOMERS_ID);

        mUriMatcher.addURI(AUTHORITY, "customers/purchases", CUSTOMER_PURCHASES);
        mUriMatcher.addURI(AUTHORITY, "customers/#/purchases", CUSTOMER_ID_PURCHASES);

        mUriMatcher.addURI(AUTHORITY, "customers/credits", CUSTOMER_CREDITS);
        mUriMatcher.addURI(AUTHORITY, "customers/#/credits", CUSTOMER_ID_CREDITS);

        mUriMatcher.addURI(AUTHORITY, "customers/transactions", CUSTOMER_TRANSACTIONS);
        mUriMatcher.addURI(AUTHORITY, "customers/#/transactions", CUSTOMER_ID_TRANSACTIONS);

        mUriMatcher.addURI(AUTHORITY, "customers/purchases/purchase_items", CUSTOMER_PURCHASE_PURCHASE_ITEMS);
        mUriMatcher.addURI(AUTHORITY, "customers/#/purchases/#/purchase_items", CUSTOMER_ID_PURCHASE_ID_PURCHASE_ITEMS);
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new AccountingDbHelper(getContext(),
                AccountingDbHelper.DATABASE_NAME, null, AccountingDbHelper.DATABASE_VERSION);
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();

        int uriType = mUriMatcher.match(uri);
        switch (uriType) {
            case CUSTOMERS_ID:
                builder.appendWhere(AccountingDbHelper.ID + "=" + uri.getLastPathSegment());
            case CUSTOMERS:
                builder.setTables(AccountingDbHelper.CUSTOMER_DUE_VIEW);
                break;

            case CUSTOMER_ID_PURCHASES:
                builder.appendWhere(AccountingDbHelper.TABLE_CUSTOMER
                        + "." + AccountingDbHelper.ID + "=" + uri.getPathSegments().get(1));
            case CUSTOMER_PURCHASES:
                builder.setTables(AccountingDbHelper.TABLE_CUSTOMER
                        + " INNER JOIN " + AccountingDbHelper.CALCULATED_PURCHASE_VIEW
                        + " ON " + AccountingDbHelper.TABLE_CUSTOMER + "." + AccountingDbHelper.ID
                        + " = " + AccountingDbHelper.CALCULATED_PURCHASE_VIEW + "." + AccountingDbHelper.PURCHASE_COL_CUSTOMER_ID);
                break;

            case CUSTOMER_ID_CREDITS:
                builder.appendWhere(AccountingDbHelper.TABLE_CUSTOMER
                        + "." + AccountingDbHelper.ID + "=" + uri.getPathSegments().get(1));
            case CUSTOMER_CREDITS:
                builder.setTables(AccountingDbHelper.TABLE_CUSTOMER
                        + " INNER JOIN " + AccountingDbHelper.TABLE_CREDIT
                        + " ON " + AccountingDbHelper.TABLE_CUSTOMER + "." + AccountingDbHelper.ID
                        + " = " + AccountingDbHelper.TABLE_CREDIT + "." + AccountingDbHelper.CREDIT_COL_CUSTOMER_ID);
                break;

            case CUSTOMER_ID_TRANSACTIONS:
                builder.appendWhere(AccountingDbHelper.CREDIT_COL_CUSTOMER_ID
                        + "=" + uri.getPathSegments().get(1));
            case CUSTOMER_TRANSACTIONS:
                // Order of columns should be same and names should match.
                String union = builder.buildUnionQuery(new String[]{
                        "SELECT " + AccountingDbHelper.ID + ","
                                + AccountingDbHelper.CPV_AMOUNT + ","
                                + AccountingDbHelper.PURCHASE_COL_DATE + ","
                                + AccountingDbHelper.PURCHASE_COL_REMARKS + ","
                                + AccountingDbHelper.PURCHASE_COL_CUSTOMER_ID
                                + " FROM " + AccountingDbHelper.CALCULATED_PURCHASE_VIEW,
                        "SELECT " + AccountingDbHelper.ID + ","
                                + AccountingDbHelper.CREDIT_COL_AMOUNT + ","
                                + AccountingDbHelper.CREDIT_COL_DATE + ","
                                + AccountingDbHelper.CREDIT_COL_REMARKS + ","
                                + AccountingDbHelper.CREDIT_COL_CUSTOMER_ID
                                + " FROM " + AccountingDbHelper.TABLE_CREDIT
                }, AccountingDbHelper.CREDIT_COL_DATE, null);
                builder.setTables("(" + union + ")");
                break;

            case CUSTOMER_ID_PURCHASE_ID_PURCHASE_ITEMS:
                builder.appendWhere(AccountingDbHelper.PURCHASE_COL_CUSTOMER_ID
                        + "=" + uri.getPathSegments().get(1)
                        + " AND "
                        + AccountingDbHelper.PI_COL_PURCHASE_ID
                        + "=" + uri.getPathSegments().get(3));
            case CUSTOMER_PURCHASE_PURCHASE_ITEMS:
                builder.setTables(AccountingDbHelper.CALCULATED_PURCHASE_VIEW
                        + " INNER JOIN " + AccountingDbHelper.TABLE_PURCHASE_ITEMS
                        + " ON " + AccountingDbHelper.CALCULATED_PURCHASE_VIEW + "." + AccountingDbHelper.ID
                        + " = " + AccountingDbHelper.PI_COL_PURCHASE_ID);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI");
        }

        Cursor cursor = builder.query(
                mDbHelper.getReadableDatabase(),
                projection, selection, selectionArgs,
                null, null, sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        return mUriMatcher.match(uri) == CUSTOMERS_ID ? CONTENT_ITEM_TYPE : CONTENT_TYPE;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        switch (mUriMatcher.match(uri)) {
            case CUSTOMERS:
                long id = mDbHelper.getWritableDatabase().insert(
                        AccountingDbHelper.TABLE_CUSTOMER, "", contentValues);
                if (id < 0) {
                    throw new SQLException("Failed to add customer!");
                }
                Uri change = ContentUris.withAppendedId(uri, id);
                getContext().getContentResolver().notifyChange(change, null);
                return change;
            case CUSTOMER_ID_PURCHASES:
                id = mDbHelper.getWritableDatabase().insert(
                        AccountingDbHelper.TABLE_PURCHASE, "", contentValues);
                if (id < 0) {
                    throw new SQLException("Failed to add purchase!");
                }
                change = ContentUris.withAppendedId(uri, id);
                getContext().getContentResolver().notifyChange(change, null);
                return change;
            case CUSTOMER_ID_CREDITS:
                id = mDbHelper.getWritableDatabase().insert(
                        AccountingDbHelper.TABLE_CREDIT, "", contentValues);
                if (id < 0) {
                    throw new SQLException("Failed to add purchase!");
                }
                change = ContentUris.withAppendedId(uri, id);
                getContext().getContentResolver().notifyChange(change, null);
                return change;
            case CUSTOMER_ID_PURCHASE_ID_PURCHASE_ITEMS:
                id = mDbHelper.getWritableDatabase().insert(
                        AccountingDbHelper.TABLE_PURCHASE_ITEMS, "", contentValues);
                if (id < 0) {
                    throw new SQLException("Failed to add purchase!");
                }
                change = ContentUris.withAppendedId(uri, id);
                getContext().getContentResolver().notifyChange(change, null);
                return change;
        }
        return null;
    }

    @Override
    public int delete(Uri uri, String s, String[] strings) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
        return 0;
    }
}