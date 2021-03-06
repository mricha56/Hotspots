package com.teamhotspots.hotspots;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.io.OutputStream;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.app.Activity.RESULT_OK;
import static android.content.Context.MODE_PRIVATE;


public class Settings extends Fragment {
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_IMAGE_PICKER = 2;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private boolean permission = false;
    private String imageUrl;
    Uri mPhotoUri;
    FirebaseUser user;
    private StorageReference mStorage;

    public Settings() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View rootView = inflater.inflate(R.layout.fragment_settings, container, false);
        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        toolbar.setTitle("Settings");
        NavigationView navigationView = (NavigationView) getActivity().findViewById(R.id.nav_view);
        navigationView.getMenu().getItem(3).setChecked(true);

        mRequestExternal();

        user = FirebaseAuth.getInstance().getCurrentUser();

        final EditText et = (EditText) rootView.findViewById(R.id.set_user_enter);
        et.setText(user.getDisplayName());
        et.setSelection(et.getText().length());

        Uri imgpath = user.getPhotoUrl();
        ImageView photoView = (ImageView) rootView.findViewById(R.id.icon);
        if (imgpath != null) {
            Picasso.with(getContext()).load(imgpath).into(photoView);
        }

        final Button button_cancel = (Button) rootView.findViewById(R.id.settings_btn_cancel);
        button_cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        final Button button_save = (Button) rootView.findViewById(R.id.settings_btn_save);
        button_save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (et.getText().length() == 0) {
                    Toast.makeText(getActivity(), "Username cannot be empty",
                            Toast.LENGTH_LONG).show();
                } else {
                    user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null) {
                        UserProfileChangeRequest profupdate = new UserProfileChangeRequest.Builder()
                                .setDisplayName(et.getText().toString()).build();
                        user.updateProfile(profupdate)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Log.d("USER", "User profile updated.");
                                        }
                                    }
                                });
                    }

                    try {
                        StorageReference filepath = mStorage.child("Icons").child(mPhotoUri.getLastPathSegment());

                        filepath.putFile(mPhotoUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            @SuppressWarnings("VisibleForTests")
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                imageUrl = taskSnapshot.getDownloadUrl().toString();
                                if (user != null) {
                                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                            .setPhotoUri(Uri.parse(imageUrl))
                                            .build();

                                    user.updateProfile(profileUpdates)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        Log.d("USER", "User profile updated.");
                                                    }
                                                }
                                            });
                                }

                            }
                        });



                    } catch (NullPointerException e) {}
                    Toast.makeText(getActivity(), "Saved",
                            Toast.LENGTH_LONG).show();

                }
            }
        });

        final LinearLayout photoLayout = (LinearLayout) rootView.findViewById(R.id.photoLayout);
        photoLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (permission) {
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    mPhotoUri = getContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            new ContentValues());
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mPhotoUri);
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                } else {
                    Toast.makeText(getContext(), "Require External Storage Permissions!", Toast.LENGTH_SHORT);
                }
            }
        });

        final LinearLayout galleryLayout = (LinearLayout) rootView.findViewById(R.id.galleryLayout);
        galleryLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (permission) {
                    Intent intent = new Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                    intent.setType("image/*");
                    startActivityForResult(intent, REQUEST_IMAGE_PICKER);
                } else {
                    Toast.makeText(getContext(), "Require External Storage Permissions!", Toast.LENGTH_SHORT);
                }

            }
        });

        // firebase
        mStorage = FirebaseStorage.getInstance().getReference();


        return rootView;
    }

    private void mRequestExternal() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            permission = true;
        } else if (ContextCompat.checkSelfPermission(getActivity(), WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            permission = true;
        } else if (shouldShowRequestPermissionRationale(WRITE_EXTERNAL_STORAGE)) {
            Snackbar.make(getActivity().findViewById(android.R.id.content)
                    , R.string.permission_rationale_external, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View v) {
                            requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                        }
                    });
        } else {
            requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                permission = true;
            }
        }
    }


    @Override
    public void onResume() {
        super.onResume();

        // try setting icon
        Bitmap bitmap = null;
        try {
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getActivity().getApplicationContext().getContentResolver(), mPhotoUri);
            } catch (IOException exception) {
            }

            int orientation = 0;

            try {
                ExifInterface exif = new ExifInterface(getPath(mPhotoUri));
                orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
                Log.d("EXIF", "Exif: " + orientation);
            } catch (Exception e) {
            }

            bitmap = rotateImage(orientation, bitmap);

            try {
                OutputStream os= getContext().getContentResolver().openOutputStream(mPhotoUri);
                bitmap.compress(Bitmap.CompressFormat.PNG,50,os);
            } catch (Exception e) {
            }

            ImageView photoView = (ImageView) getView().findViewById(R.id.icon);
            photoView.setImageBitmap(bitmap);
        } catch (NullPointerException exception) {}
    }

    private String getPath(Uri uri) {
        String[]  data = { MediaStore.Images.Media.DATA };
        CursorLoader loader = new CursorLoader(getContext(), uri, data, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    public Bitmap rotateImage(int orientation, Bitmap bitmap) {
        Matrix matrix = new Matrix();
        if (orientation == 6) {
            matrix.postRotate(90);
        }
        else if (orientation == 3) {
            matrix.postRotate(180);
        }
        else if (orientation == 8) {
            matrix.postRotate(270);
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true); // rotating bitmap
    }


    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            //Uri uri = data.getData();

        } else if (requestCode == REQUEST_IMAGE_PICKER && resultCode == RESULT_OK) {
            mPhotoUri = data.getData();
        }
    }
}
