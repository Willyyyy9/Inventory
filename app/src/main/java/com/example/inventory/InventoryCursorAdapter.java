package com.example.inventory;


import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;
import com.example.inventory.data.InventoryContract.ProductEntry;


public class InventoryCursorAdapter extends CursorAdapter {
    //Public Constructor
    public InventoryCursorAdapter(Context context, Cursor c) { super(context, c,0); }


    String quantityString = "";


    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        //inflating the list_item.xml
        return LayoutInflater.from(context).inflate(R.layout.list_item,parent,false);
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {



        //initializing the views in the list_item.xml
        TextView nameTextView = view.findViewById(R.id.name);
        TextView quantityTextView = view.findViewById(R.id.quantity);
        TextView priceTextView = view.findViewById(R.id.price);
        Button soldOneUnit = view.findViewById(R.id.soldOneUnit);

        //getting the column indexes of the data we want to display
        int columnIdIndex = cursor.getColumnIndex(ProductEntry._ID);
        int nameColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
        int quantityColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QUANTITY);
        int priceColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE);

        //Getting the data from the cursor
        final String column = cursor.getString(columnIdIndex);
        String productName = cursor.getString(nameColumnIndex);
        final int quantity = cursor.getInt(quantityColumnIndex);
        int price = cursor.getInt(priceColumnIndex);


        if(quantity == 0){ quantityString = "Out of stock";
        }else{ quantityString = quantity+ " units left."; }

        //putting the requested data in the views
        String priceString = price + "$ per unit.";
        nameTextView.setText(productName);
        quantityTextView.setText(quantityString);
        priceTextView.setText(priceString);








        soldOneUnit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Decreasing the quantity by one unit when the button is clicked
                MainActivity mainActivity = (MainActivity) context ;
                mainActivity.decreaseCount(Integer.valueOf(column),quantity);
            }
        });
    }
}
