package com.example.inventory;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.inventory.data.InventoryContract;
import com.example.inventory.data.InventoryContract.ProductEntry;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int PRODUCT_LOADER = 0;
    InventoryCursorAdapter inventoryCursorAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Setting the FloatingActionButton
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Goes to the editor activity once clicked
                Intent intent = new Intent(MainActivity.this, EditorActivity.class);
                startActivity(intent);
            }
        });

        //Setting the listview that will show the products
        ListView productsListView = findViewById(R.id.list);
        inventoryCursorAdapter = new InventoryCursorAdapter(this, null);
        productsListView.setAdapter(inventoryCursorAdapter);
        productsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this,EditorActivity.class);
                Uri currentProductUri = ContentUris.withAppendedId(ProductEntry.CONTENT_URI, id);
                intent.setData(currentProductUri);
                startActivity(intent);
            }
        });

        //Setting the view that will appear if the list is empty
        View emptyView = findViewById(R.id.empty_view);
        productsListView.setEmptyView(emptyView);

        //Loading the data via the Loader in a background thread
        getSupportLoaderManager().initLoader(PRODUCT_LOADER,null,this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_catalog.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Delete all entries" menu option
            case R.id.action_delete_all_entries:
                deleteAllProducts();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteAllProducts() {
        int rowsDeleted = getContentResolver().delete(ProductEntry.CONTENT_URI, null, null);
        Log.v("MainActivity", rowsDeleted + " rows deleted from product database");
    }





    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {

        // An array with the needed columns in the database to display in the list
        String[] projection = {
                ProductEntry._ID,
                ProductEntry.COLUMN_PRODUCT_NAME,
                ProductEntry.COLUMN_PRODUCT_QUANTITY,
                ProductEntry.COLUMN_PRODUCT_PRICE};
        //Requesting the data needed through the CursorLoader
        return new CursorLoader( this,ProductEntry.CONTENT_URI,projection,null,null,null);

    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        //Displaying the data through the custom adapter
        inventoryCursorAdapter.swapCursor(data);

    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        //Setting the custom adapter to null when the loader is reset
        inventoryCursorAdapter.swapCursor(null);

    }

    public void decreaseCount(int columnId, int quantity){

        //Decreasing the quantity of the product by one and updating the new value in the database
        //This function is used in the onCLickListener of the button put in every list item
        if(quantity-1 < 0){
            //if the quantity decreased by one will be less than zero, then a Toast will be displayed
            Toast.makeText(this, "This product is out of stock", Toast.LENGTH_SHORT).show();
        }else {
            //if the quantity decreased by one will be equal or more than zero than we update the new value in the database
            quantity = quantity - 1;
            ContentValues values = new ContentValues();
            values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, quantity);
            Uri updateUri = ContentUris.withAppendedId(ProductEntry.CONTENT_URI, columnId);
            int rowsAffected = getContentResolver().update(updateUri, values,null, null);
        }
    }
}
