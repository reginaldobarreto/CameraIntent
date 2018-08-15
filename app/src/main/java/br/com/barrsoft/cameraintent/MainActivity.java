package br.com.barrsoft.cameraintent;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_CAMERA = 99;
    private String [] permissions={Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE};
    @BindView(R.id.button) Button action;
    @BindView(R.id.imageView) ImageView imageView;
    private String absoluteImageLocation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);
        PermissaoRequest.onRequestPermissions(REQUEST_CODE_CAMERA,this,permissions);
    }

    @OnClick (R.id.button)
    void takePicture(View view){

        Intent intentCamera = new Intent();
        intentCamera.setAction(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        File photofile = null;
        try {
            photofile = createImageFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String authority = getApplicationContext().getPackageName() + ".fileprovider";

        //intentCamera.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photofile));// metodo nao aceito > V6
        intentCamera.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(this,authority,photofile));
        startActivityForResult(intentCamera, REQUEST_CODE_CAMERA);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(requestCode == REQUEST_CODE_CAMERA && resultCode == Activity.RESULT_OK){

            Bitmap photo = BitmapFactory.decodeFile(absoluteImageLocation);
            //Glide.with(this).asBitmap().load(photo).into(imageView);

            rotatePicture(photo);

        }
    }

    File createImageFile() throws IOException {


        String imageFileName = "PICTURE_";
        File storageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName,".jpg",storageDirectory);

        absoluteImageLocation = image.getAbsolutePath();

        return image;
    }

    void resizePicture(){

        int destinationHeight = imageView.getHeight();
        int destinationWidht = imageView.getHeight();

        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(absoluteImageLocation,bitmapOptions);

        int cameraSizePictureHeight = bitmapOptions.outHeight;
        int cameraSizePictureWidth = bitmapOptions.outWidth;

        int scalaPicture = Math.min(cameraSizePictureHeight/destinationHeight , cameraSizePictureWidth/destinationWidht);
        bitmapOptions.inSampleSize = scalaPicture;
        bitmapOptions.inJustDecodeBounds = false;
        Bitmap pictureResize = BitmapFactory.decodeFile(absoluteImageLocation);

    }

    private void rotatePicture(Bitmap bitmap){

        ExifInterface exifInterface = null;
        try{
            exifInterface = new ExifInterface(absoluteImageLocation);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(270);
                break;
        }
        Bitmap rotatePicture = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);


        Glide.with(this).asBitmap().load(rotatePicture).into(imageView);

    }

    public static class PermissaoRequest {

        public static boolean onRequestPermissions(int requestCode, Activity activity, String[] permissions) {

            //checar a versao SDK
            if(Build.VERSION.SDK_INT >= 23){

                //criar uma lista de permissao
                List<String> listPermission = new ArrayList<>();

                //varrer permissoes liberadas
                for (String permissionRequest: permissions) {

                    //checar as permissoes requeridas pela activity confrontando ContextCompat.checkSelfPermission==PackageManager.PERMISSION_GRANTED, armazenar em boolean
                    boolean checkPermission = ContextCompat.checkSelfPermission
                            (activity, permissionRequest) == PackageManager.PERMISSION_GRANTED;

                    //adicionar permissoes pendentes na lista de permissao
                    if(!checkPermission){
                        listPermission.add(permissionRequest);
                    }
                }

                //lista vazia nao solicite permissao
                if (listPermission.isEmpty()){
                    //tudo certinho, so retornar verdadeiro
                    return true;
                }

                //o metodo requestPermissions recebe como parametro um array, entao devemos converter um list em array
                String[] permissionRequest = new String[listPermission.size()];
                permissionRequest = listPermission.toArray(permissionRequest);

                //lista preenchida solicitar permissao usando ActivityCompat
                ActivityCompat.requestPermissions(activity, permissionRequest, requestCode);
            }
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        for (int resultado: grantResults) {

            if(resultado == PackageManager.PERMISSION_DENIED){
                alertaValidacaoPermissao();
            }
        }

    }

    private void alertaValidacaoPermissao() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permissão Negada");
        builder.setMessage("Para utilizar esse app, é necessário aceitar as permissões");
        builder.setPositiveButton("Confirmar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        builder.create().show();
    }

}
