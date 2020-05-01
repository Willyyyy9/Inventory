package com.example.inventory;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import com.example.inventory.data.InventoryContract.ProductEntry;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    //Setting the global variables needed in the class
    private static final int EXISTING_PRODUCT_LOADER = 0;
    private static final int REQUEST_IMAGE = 100;
    private String nameString;
    private String supplierString;
    private String quantityString;
    private String priceString;
    private int quantityInteger;
    private int priceInteger;
    private int quantityQueried;
    private int priceQueried;
    private Uri mCurrentProductUri;
    private EditText mNameEditText;
    private EditText mSupplierEditText;
    private EditText mQuantityEditText;
    private EditText mPriceEditText;
    private ImageButton addImageButton;
    private RadioGroup radioGroup;
    private Uri imageFilePath;
    private Bitmap imageToStore;
    private ByteArrayOutputStream outputStream;
    private byte[] imageInBytes;
    private boolean mProductHasChanged = false;
    private boolean addToStock = false;
    private boolean removeFromStock = false;


    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mProductHasChanged = true;
            return false;
        }
    };





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        //Initializing the EditTexts
        mNameEditText = findViewById(R.id.nameEditText);
        mSupplierEditText = findViewById(R.id.supplierEditText);
        mQuantityEditText = findViewById(R.id.quantityEditText);
        mPriceEditText = findViewById(R.id.priceEditText);
        addImageButton = findViewById(R.id.addImageButton);

        //Setting onTouchListeners to the EditTexts
        mNameEditText.setOnTouchListener(mTouchListener);
        mSupplierEditText.setOnTouchListener(mTouchListener);
        mQuantityEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);

        //Initializing the RadioGroup
        radioGroup = findViewById(R.id.radioGroup);


        //Getting the product URI sent with the intent from the MainActivity
        Intent intent = getIntent();
        mCurrentProductUri = intent.getData();
        if (mCurrentProductUri == null) {
            //If the URI extracted is null, then the user wants to add a new product
            //The title will be Add a product and the optionMenu will be disabled

            setTitle("Add a product");
            invalidateOptionsMenu();
            radioGroup.setVisibility(View.GONE);
        } else {
            //if the URI is not null, the data related to a single product will be loaded through initLoader()
            //The title will be Add a product and the radioGroup will be visible
            setTitle("Edit product");
            radioGroup.setVisibility(View.VISIBLE);
            getLoaderManager().initLoader(EXISTING_PRODUCT_LOADER, null, this);
        }

        addImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Selecting an image from the local storage of the device
                Intent objectIntent = new Intent();
                objectIntent.setType("image/*");
                objectIntent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(objectIntent,REQUEST_IMAGE);
            }
        });

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                //Knowing if the user wants to add or remove from the stock
                switch(checkedId){
                    case R.id.addToStockRadioButton:
                        addToStock = true;
                        break;
                    case R.id.removeFromStockRadioButton:
                        removeFromStock = true;
                        break;

                }
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null){
            try{
                //Getting the response from the local storage of the device
                //Converting the image chosen from a URI to a byte[] so we can store it in the database
                imageFilePath = data.getData();
                imageToStore = MediaStore.Images.Media.getBitmap(getContentResolver(),imageFilePath);
                outputStream = new ByteArrayOutputStream();
                imageToStore.compress(Bitmap.CompressFormat.PNG,100,outputStream);
                imageInBytes = outputStream.toByteArray();
                //Setting the image button with the selected image
                addImageButton.setImageBitmap(imageToStore);

            }catch (Exception e){
                Toast.makeText(this,e.getMessage(),Toast.LENGTH_SHORT).show();
            }


        }
    }

    private void saveProduct(){

        //Getting data from the EditTexts
        nameString = mNameEditText.getText().toString().trim();
        supplierString = mSupplierEditText.getText().toString().trim();
        quantityString = mQuantityEditText.getText().toString().trim();
        quantityInteger = Integer.parseInt(quantityString);
        priceString = mPriceEditText.getText().toString().trim();
        priceInteger = Integer.parseInt(priceString);

        if (mCurrentProductUri == null && TextUtils.isEmpty(nameString) && TextUtils.isEmpty(supplierString) && TextUtils.isEmpty(priceString) && imageInBytes == null) {
            Log.e("Editor Activity","first part of saveProduct() returned nothing");
            return;
        }

        //Putting the extracted values into a ContentValues object
        ContentValues values = new ContentValues();
        values.put(ProductEntry.COLUMN_PRODUCT_NAME, nameString);
        values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, quantityInteger);
        values.put(ProductEntry.COLUMN_PRODUCT_PRICE, priceInteger);
        values.put(ProductEntry.COLUMN_PRODUCT_SUPPLIER, supplierString);
        values.put(ProductEntry.COLUMN_PRODUCT_IMAGE, imageInBytes);




        if (mCurrentProductUri == null) {
            // This is a NEW product, so insert a new product into the provider,
            // returning the content URI for the new product.
            Uri newUri = getContentResolver().insert(ProductEntry.CONTENT_URI, values);
            // Show a toast message depending on whether or not the insertion was successful.
            if (newUri == null) {
                // If the new content URI is null, then there was an error with insertion.
                Toast.makeText(this, getString(R.string.editor_insert_product_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the insertion was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_insert_product_successful),
                        Toast.LENGTH_SHORT).show();


            }
        } else {

            if(addToStock){
                //the user want to add a quantity
                values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY,quantityQueried + quantityInteger);
                addToStock = false;
                int rowsAffected = getContentResolver().update(mCurrentProductUri, values, null, null);
                // Show a toast message depending on whether or not the update was successful.
                if (rowsAffected == 0) {
                    // If no rows were affected, then there was an error with the update.
                    Toast.makeText(this, getString(R.string.editor_update_product_failed),
                            Toast.LENGTH_SHORT).show();
                } else {
                    // Otherwise, the update was successful and we can display a toast.
                    Toast.makeText(this, getString(R.string.editor_update_product_successful),
                            Toast.LENGTH_SHORT).show();
                }
            }else if(removeFromStock) {
                //The user want to remove a quantity
                if(quantityQueried - quantityInteger < 0){
                    //if the amount the user want removed exceeds the amount available, a toast will show
                    Toast.makeText(this, getString(R.string.insufficient_amount), Toast.LENGTH_SHORT).show();
                }else{
                    //putting the updated quantity into a ContentValues and updating the database
                    values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY,quantityQueried - quantityInteger);
                    removeFromStock = false;
                    int rowsAffected = getContentResolver().update(mCurrentProductUri, values, null, null);
                    if (rowsAffected == 0) {
                        // If no rows were affected, then there was an error with the update.
                        Toast.makeText(this, getString(R.string.editor_update_product_failed),
                                Toast.LENGTH_SHORT).show();
                    } else {
                        // Otherwise, the update was successful and we can display a toast.
                        Toast.makeText(this, getString(R.string.editor_update_product_successful),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }else {
                //The user want to put a specific quantity in the database
                int rowsAffected = getContentResolver().update(mCurrentProductUri, values, null, null);
                if (rowsAffected == 0) {
                    // If no rows were affected, then there was an error with the update.
                    Toast.makeText(this, getString(R.string.editor_update_product_failed),
                            Toast.LENGTH_SHORT).show();
                } else {
                    // Otherwise, the update was successful and we can display a toast.
                    Toast.makeText(this, getString(R.string.editor_update_product_successful),
                            Toast.LENGTH_SHORT).show();
                }
            }
        }


    }

    private void showUnsavedChangesDialog(
            //Shows a AlertDialog to see if the user wants to exit the activity or continue editing
            DialogInterface.OnClickListener discardButtonClickListener) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.unsaved_changes_dialog_msg);
            builder.setPositiveButton(R.string.discard, discardButtonClickListener);
            builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                    if (dialog != null) {
                        dialog.dismiss();
                    }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void onBackPressed() {
        // If the product hasn't changed, continue with handling back button press
        if (!mProductHasChanged) {
            super.onBackPressed();
            return;
        }
        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new pet, hide the "Delete" menu item.
        if (mCurrentProductUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                saveProduct();
                finish();
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // Navigate back to parent activity (MainActivity)
                if (!mProductHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the product.
                deleteProduct();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the product.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void deleteProduct() {
        // Only perform the delete if this is an existing product.
        if (mCurrentProductUri != null) {
            // Call the ContentResolver to delete the product at the given content URI.
            // Pass in null for the selection and selection args because the mCurrentProductUri
            // content URI already identifies the product that we want.
            int rowsDeleted = getContentResolver().delete(mCurrentProductUri, null, null);

            // Show a toast message depending on whether or not the delete was successful.
            if (rowsDeleted == 0) {
                // If no rows were deleted, then there was an error with the delete.
                Toast.makeText(this, getString(R.string.editor_delete_product_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the delete was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_delete_product_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }
        finish();
    }



    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {
                ProductEntry._ID,
                ProductEntry.COLUMN_PRODUCT_NAME,
                ProductEntry.COLUMN_PRODUCT_QUANTITY,
                ProductEntry.COLUMN_PRODUCT_PRICE,
                ProductEntry.COLUMN_PRODUCT_SUPPLIER,
                ProductEntry.COLUMN_PRODUCT_IMAGE};
        return new CursorLoader(this,    // Parent activity context
                mCurrentProductUri, projection,  // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                // No selection arguments
                null);

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        if (cursor.moveToFirst()) {

            //getting the column indexes of the columns in the cursor
            int nameColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
            int quantityColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QUANTITY);
            int priceColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE);
            int supplierColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_SUPPLIER);
            int imageColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_IMAGE);

            //getting data from the cursor
            String name = cursor.getString(nameColumnIndex);
            quantityQueried = cursor.getInt(quantityColumnIndex);
            priceQueried = cursor.getInt(priceColumnIndex);
            String supplier = cursor.getString(supplierColumnIndex);
            imageInBytes = cursor.getBlob(imageColumnIndex);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(imageInBytes);
            Bitmap imageBitmap= BitmapFactory.decodeStream(inputStream);

            //setting the requested data in the edittexts
            mNameEditText.setText(name);
            mQuantityEditText.setText(Integer.toString(quantityQueried));
            mPriceEditText.setText(Integer.toString(priceQueried));
            mSupplierEditText.setText(supplier);
            addImageButton.setImageBitmap(imageBitmap);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        //setting the edittexts to contain no information when the loader is reset
        mNameEditText.setText("");
        mQuantityEditText.setText("");
        mPriceEditText.setText("");
        mSupplierEditText.setText("");
        addImageButton.setImageResource(R.drawable.addphoto);
    }
}
