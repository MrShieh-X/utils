//package com.mrshiehx.minecraft_versions_viewer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.os.Looper;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Patterns;
import android.webkit.URLUtil;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class UtilsForMCVV {
    static String authorMail = "bntoylort@outlook.com";

    public static File createFile(String path, String fileName) {
        return new File(path, fileName);
    }

    public static String getAndroidDirCachePath(Context context) {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) || !Environment.isExternalStorageRemovable()) {
            return context.getExternalCacheDir().getPath();
        } else {
            return context.getCacheDir().getPath();
        }
    }

    public static String getDataFilesPath(Context context) {
        return context.getFilesDir().getAbsolutePath();
    }

    public static String getFileExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    public static String getDataCachePath(Context context) {
        return context.getCacheDir().getAbsolutePath();
    }

    public static boolean fileIsExists(Context context, String filePathAndName) {
        try {
            File f = new File(filePathAndName);
            if (!f.exists()) {
                return false;
            }
        } catch (Exception e) {
            UtilsForMCVV.exceptionDialog(context, e);
            return false;
        }
        return true;
    }

    public static void deleteFile(Context context, String filePathAndName) {
        try {
            File f = new File(filePathAndName);
            if (f.exists()) {
                if (f.delete()) {
                    Log.i("MSVV.UtilsForMCVV.deleteFile", "Delete file (" + filePathAndName + ") is success!");
                } else {
                    showDialog(context, "", "delete failed");
                    Log.e("MSVV.UtilsForMCVV.deleteFile", "Delete file (" + filePathAndName + ") is failed!");
                }
            } else {
                Log.e("MSVV.UtilsForMCVV.deleteFile", "Delete file (" + filePathAndName + ") is not exists!");
            }
        } catch (Exception e) {
            Looper.prepare();
            UtilsForMCVV.exceptionDialog(context, e);
            Looper.loop();
        }
    }

    public static void downloadAndInitListview(final Context context, final String downloadFileUrl, final String afterDownloadFileName, final String downloadToPath, final List<VersionItem> listviewItem, final String needLoadedFilePathAndName, final String needLoadedFileArrayName) throws JSONException {
        if (isNetworkConnected(context) == true) {
            File file = new File(getDataFilesPath(context));
            if (!file.exists()) {
                file.mkdirs();
            }
            final ProgressDialog waitDownloadAndInitialization = new ProgressDialog(context);
            waitDownloadAndInitialization.setTitle(context.getResources().getString(R.string.dialog_wait_download_and_initialization_title));
            waitDownloadAndInitialization.setMessage(context.getResources().getString(R.string.dialog_wait_download_and_initialization_message));
            waitDownloadAndInitialization.setCancelable(false);
            waitDownloadAndInitialization.show();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        URL url = new URL(downloadFileUrl);
                        HttpURLConnection con = (HttpURLConnection) url.openConnection();
                        con.setReadTimeout(5000);
                        con.setConnectTimeout(5000);
                        con.setRequestProperty("Charset", "UTF-8");
                        con.setRequestMethod("GET");
                        if (con.getResponseCode() == 200) {
                            InputStream is = con.getInputStream();
                            FileOutputStream fileOutputStream = null;
                            if (is != null) {
                                //FileUtilsForMCVV fileUtilsForMCVV = new FileUtilsForMCVV();
                                fileOutputStream = new FileOutputStream(createFile(downloadToPath, afterDownloadFileName));
                                byte[] buf = new byte[1024];
                                int ch;
                                while ((ch = is.read(buf)) != -1) {
                                    fileOutputStream.write(buf, 0, ch);
                                }
                            }
                            if (fileOutputStream != null) {
                                fileOutputStream.flush();
                                fileOutputStream.close();
                            }
                        }

                        final Timer timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                if (UtilsForMCVV.fileIsExists(context, downloadToPath + "/" + afterDownloadFileName) == true) {
                                    waitDownloadAndInitialization.dismiss();
                                    timer.cancel();
                                    try {
                                        initListview(context, listviewItem, needLoadedFilePathAndName, needLoadedFileArrayName);
                                    } catch (JSONException e) {
                                        Looper.prepare();
                                        exceptionDialog(context, e, context.getResources().getString(R.string.dialog_exception_initlistviewfailed));
                                        Looper.loop();
                                    }

                                } else {

                                }
                            }
                        }, 0, 100);


                    } catch (Exception e) {

                        Looper.prepare();
                        exceptionDialog(context, e, context.getResources().getString(R.string.dialog_exception_downloadfailed));
                        Looper.loop();
                    }

                }
            }).start();
        } else {
            if (fileIsExists(context, needLoadedFilePathAndName) == true) {
                initListview(context, listviewItem, needLoadedFilePathAndName, needLoadedFileArrayName);
            }

        }
    }

    public static void downloadFile(final Context context, final String downloadFileUrl, final String afterDownloadFileName, final String downloadToPath) {
        if (isNetworkConnected(context) == true) {
            File file = new File(getDataFilesPath(context));
            if (!file.exists()) {
                file.mkdirs();
            }
            final ProgressDialog waitDownload = new ProgressDialog(context);
            waitDownload.setTitle(context.getResources().getString(R.string.dialog_wait_download_and_initialization_title));
            waitDownload.setMessage(context.getResources().getString(R.string.dialog_wait_download_and_initialization_message));
            waitDownload.setCancelable(false);
            waitDownload.show();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        URL url = new URL(downloadFileUrl);
                        HttpURLConnection con = (HttpURLConnection) url.openConnection();
                        con.setReadTimeout(5000);
                        con.setConnectTimeout(5000);
                        con.setRequestProperty("Charset", "UTF-8");
                        con.setRequestMethod("GET");
                        if (con.getResponseCode() == 200) {
                            InputStream is = con.getInputStream();
                            FileOutputStream fileOutputStream = null;
                            if (is != null) {
                                //FileUtilsForMCVV fileUtilsForMCVV = new FileUtilsForMCVV();
                                fileOutputStream = new FileOutputStream(createFile(downloadToPath, afterDownloadFileName));
                                byte[] buf = new byte[1024];
                                int ch;
                                while ((ch = is.read(buf)) != -1) {
                                    fileOutputStream.write(buf, 0, ch);
                                }
                            }
                            if (fileOutputStream != null) {
                                fileOutputStream.flush();
                                fileOutputStream.close();
                            }
                        }
                        final Timer timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                if (UtilsForMCVV.fileIsExists(context, downloadToPath + "/" + afterDownloadFileName) == true) {
                                    waitDownload.dismiss();
                                    timer.cancel();
                                } else {

                                }
                            }
                        }, 0, 100);


                    } catch (Exception e) {

                        Looper.prepare();
                        exceptionDialog(context, e, context.getResources().getString(R.string.dialog_exception_downloadfailed));
                        Looper.loop();
                    }
                }
            }).start();
        } else {
            Toast.makeText(context, context.getResources().getString(R.string.toast_please_check_your_network), Toast.LENGTH_SHORT).show();
        }
    }

    public static String getJson(Context context, String filePathAndName) {
        // 将json数据变成字符串
        StringBuilder stringBuilder = new StringBuilder();
        // 获得assets资源管理器
        //AssetManager assetManager = context.getAssets();
        // 使用IO流读取json文件内容
        try {
            //BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(assetManager.open(fileName), "utf-8"));
            BufferedReader bufferedReader = new BufferedReader(new FileReader(filePathAndName));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line.trim());
            }
        } catch (IOException e) {
            exceptionDialog(context, e, context.getResources().getString(R.string.dialog_exception_getjsonfailed));
        }
        return stringBuilder.toString();
    }

    public static String getJsonByAssets(Context context, String fileName) {
        // 将json数据变成字符串
        StringBuilder stringBuilder = new StringBuilder();
        // 获得assets资源管理器
        AssetManager assetManager = context.getAssets();
        // 使用IO流读取json文件内容
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(assetManager.open(fileName), "utf-8"));
            //BufferedReader bufferedReader = new BufferedReader(new FileReader(filePathAndName));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line.trim());
            }
        } catch (IOException e) {
            exceptionDialog(context, e, context.getResources().getString(R.string.dialog_exception_getjsonfailed));
        }
        return stringBuilder.toString();
    }

    public static void showDialog(final Context context, String title, String message) {
        AlertDialog.Builder dialog =
                new AlertDialog.Builder(context);
        dialog.setTitle(title).setMessage(message);
        dialog.show();
    }

    public static void showDialog(final Context context, String title, String message, String buttonName, DialogInterface.OnClickListener buttonOnClickListener) {
        AlertDialog.Builder dialog =
                new AlertDialog.Builder(context);
        dialog.setTitle(title).setMessage(message);
        dialog.setNegativeButton(context.getResources().getString(android.R.string.cancel), null);
        dialog.setPositiveButton(buttonName, buttonOnClickListener);
        dialog.show();
    }


    public static void exceptionDialog(final Context context, final Exception exception) {
        AlertDialog.Builder dialog =
                new AlertDialog.Builder(context);

        String dialogExceptionMessage = String.format(context.getResources().getString(R.string.dialog_exception_message), context, exception);
        dialog.setTitle(context.getResources().getString(R.string.dialog_exception_title)).setMessage(dialogExceptionMessage);
        dialog.setNegativeButton(context.getResources().getString(android.R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        dialog.setPositiveButton(context.getResources().getString(R.string.dialog_exception_button_feedback), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sendMail(context, authorMail, "AN ERROR OF MCVV", "There is a problem, application package name is: (" + getPackageName(context) + "), application version name is: (" + getVersionName(context) + "), application version code is(" + getVersionCode(context) + "), android version is: (" + getSystemVersion() + "), device brand is: (" + getDeviceBrand() + "), device model is: (" + getDeviceModel() + "), class is: (" + context + "), error is: (" + exception + ")");
            }
        });
        dialog.show();
    }

    public static void exceptionDialog(final Context context, final Exception exception, final String detailException) {
        AlertDialog.Builder dialog =
                new AlertDialog.Builder(context);

        String dialogExceptionMessage = String.format(context.getResources().getString(R.string.dialog_exception_detail_message), context, exception, detailException);
        dialog.setTitle(context.getResources().getString(R.string.dialog_exception_title)).setMessage(dialogExceptionMessage);
        dialog.setNegativeButton(context.getResources().getString(android.R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        dialog.setPositiveButton(context.getResources().getString(R.string.dialog_exception_button_feedback), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sendMail(context, authorMail, "AN ERROR OF MCVV", "There is a problem, application package name is: (" + getPackageName(context) + "), application version name is: (" + getVersionName(context) + "), application version code is(" + getVersionCode(context) + "), android version is: (" + getSystemVersion() + "), device brand is: (" + getDeviceBrand() + "), device model is: (" + getDeviceModel() + "), class is: (" + context + "), error is: (" + exception + "), detail is:(" + detailException + ")");
            }
        });
        dialog.show();
    }


    public static boolean isNetworkConnected(Context context) {
        if (context != null) {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
            if (mNetworkInfo != null) {
                return mNetworkInfo.isAvailable();
            }
        }
        return false;
    }

    public static void sendMail(Context context, String reciver, String subject, String text) {
        Intent data = new Intent(Intent.ACTION_SENDTO);
        data.setData(Uri.parse("mailto:" + reciver));
        data.putExtra(Intent.EXTRA_SUBJECT, subject);
        data.putExtra(Intent.EXTRA_TEXT, text);
        context.startActivity(data);
    }

    public static void goToWebsite(Context context, String url) {
        Intent intent = new Intent();
        intent.setData(Uri.parse(url));
        intent.setAction(Intent.ACTION_VIEW);
        context.startActivity(intent);
    }

    public static String getSystemVersion() {
        return android.os.Build.VERSION.RELEASE;
    }

    public static String getDeviceModel() {
        return android.os.Build.MODEL;
    }

    public static String getDeviceBrand() {
        return android.os.Build.BRAND;
    }

    public static String getVersionName(Context context) {

        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo packageInfo = pm.getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (Exception e) {
            exceptionDialog(context, e);
        }

        return null;

    }

    public static int getVersionCode(Context context) {

        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo packageInfo = pm.getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (Exception e) {
            exceptionDialog(context, e);
        }

        return 0;

    }

    public static synchronized String getPackageName(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(
                    context.getPackageName(), 0);
            return packageInfo.packageName;
        } catch (Exception e) {
            exceptionDialog(context, e);
        }
        return null;
    }

    public static void initListview(Context context, List<VersionItem> listviewItem, String needLoadedFilePathAndName, String needLoadedArrayName) throws JSONException {
        String strJsondata = UtilsForMCVV.getJson(context, needLoadedFilePathAndName);
        JSONObject jsonObject = new JSONObject(strJsondata);
        JSONArray resultArray = jsonObject.getJSONArray(needLoadedArrayName);

        for (int i = 0; i < resultArray.length(); i++) {
            jsonObject = resultArray.getJSONObject(i);

            try {
                String number = jsonObject.getString("number");
                String versionName = jsonObject.getString("versionName");
                String releaseDate = jsonObject.getString("releaseDate");

                VersionItem item = new VersionItem(number, versionName, releaseDate);
                listviewItem.add(item);

            } catch (JSONException e) {
                exceptionDialog(context, e, context.getResources().getString(R.string.dialog_exception_getjsonorinitlistviewfailed));
            }

        }
    }

    public static void initialization(Activity context, int titleId) {
        //SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        initializationTheme(context);
        //setLanguage(context,sharedPreferences.getString("modify_language","en_US"));
        //String [] languageAndCountry = null;
        //languageAndCountry = sharedPreferences.getString("modify_language","en_US").split("_");
        //context.setTitle(getStringByLocale(context,titleId,languageAndCountry[0],languageAndCountry[1]));
        initializationA(context, titleId);
    }

    public static void initialization(PreferenceActivity context, int titleId) {
        initializationTheme(context);
        initializationA(context, titleId);
    }

    public static void initializationA(Activity context, int titleId) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        setLanguage(context, sharedPreferences.getString("modify_language", "en_US"));
        String[] languageAndCountry = null;
        languageAndCountry = sharedPreferences.getString("modify_language", "en_US").split("_");
        context.setTitle(getStringByLocale(context, titleId, languageAndCountry[0], languageAndCountry[1]));

    }

    public static void initializationTheme(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        if (sharedPreferences.getString("modify_theme", "light").equals("dark")) {
            context.setTheme(R.style.AppThemeDark);
        } else {
            context.setTheme(R.style.AppTheme);
        }

    }


    public static void initializationTheme(PreferenceActivity context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        if (sharedPreferences.getString("modify_theme", "light").equals("dark")) {
            context.setTheme(R.style.AppThemeDark);
            context.getListView().setBackgroundColor(Color.rgb(48, 48, 48));
        } else {
            context.setTheme(R.style.AppTheme);
        }

    }

    public static void setLanguage(Activity context, String language) {
        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();
        DisplayMetrics dm = resources.getDisplayMetrics();
        if (language.equals("zh_CN")) {
            config.locale = Locale.SIMPLIFIED_CHINESE;
        } else if (language.equals("zh_TW")) {
            config.locale = Locale.TRADITIONAL_CHINESE;
        } else {
            config.locale = Locale.ENGLISH;
        }
        resources.updateConfiguration(config, dm);
    }

    public static String getStringByLocale(Context context, int stringId, String language, String country) {
        Resources resources = getApplicationResource(context.getApplicationContext().getPackageManager(),
                getPackageName(context), new Locale(language, country));
        if (resources == null) {
            return "";
        } else {
            try {
                return resources.getString(stringId);
            } catch (Exception e) {
                return "";
            }
        }
    }

    private static Resources getApplicationResource(PackageManager pm, String pkgName, Locale l) {
        Resources resourceForApplication = null;
        try {
            resourceForApplication = pm.getResourcesForApplication(pkgName);
            updateResource(resourceForApplication, l);
        } catch (PackageManager.NameNotFoundException e) {

        }
        return resourceForApplication;
    }

    private static void updateResource(Resources resource, Locale l) {
        Configuration config = resource.getConfiguration();
        config.locale = l;
        resource.updateConfiguration(config, null);
    }

    public static boolean isUrlAUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            return URLUtil.isValidUrl(urlString) && Patterns.WEB_URL.matcher(urlString).matches();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static void downloadFileForSettings(final Context context, final String downloadFileUrl, final String afterDownloadFileName, final String downloadToPath, final String versionType) {
        if (isNetworkConnected(context) == true) {
            File file = new File(getDataFilesPath(context));
            if (!file.exists()) {
                file.mkdirs();
            }
            final ProgressDialog waitDownload = new ProgressDialog(context);
            waitDownload.setTitle(context.getResources().getString(R.string.dialog_wait_download_and_initialization_title));
            waitDownload.setMessage(context.getResources().getString(R.string.dialog_wait_download_and_initialization_message));
            waitDownload.setCancelable(false);
            waitDownload.show();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        URL url = new URL(downloadFileUrl);
                        final HttpURLConnection con = (HttpURLConnection) url.openConnection();
                        con.setReadTimeout(5000);
                        con.setConnectTimeout(5000);
                        con.setRequestProperty("Charset", "UTF-8");
                        con.setRequestMethod("GET");
                        if (con.getResponseCode() == 200) {
                            InputStream is = con.getInputStream();
                            FileOutputStream fileOutputStream = null;
                            if (is != null) {
                                //FileUtilsForMCVV fileUtilsForMCVV = new FileUtilsForMCVV();
                                fileOutputStream = new FileOutputStream(createFile(downloadToPath, afterDownloadFileName));
                                byte[] buf = new byte[1024];
                                int ch;
                                while ((ch = is.read(buf)) != -1) {
                                    fileOutputStream.write(buf, 0, ch);
                                }
                            }
                            if (fileOutputStream != null) {
                                fileOutputStream.flush();
                                fileOutputStream.close();
                            }
                        }
                        final Timer timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                if (UtilsForMCVV.fileIsExists(context, UtilsForMCVV.getDataCachePath(context) + "/" + afterDownloadFileName) == true) {
                                    //Looper.prepare();
                                    UtilsForMCVV.deleteFile(context, UtilsForMCVV.getDataCachePath(context) + "/" + afterDownloadFileName);
                                    //Looper.loop();
                                    //Looper.prepare();
                                    //有效的，所以不显示
                                    //showDialog(context,"","deleteanddownloadsucess");
                                    //Looper.loop();
                                    timer.cancel();

                                } else {
                                    Looper.prepare();
                                    if (versionType.equals(VersionsType.STABLE_RELEASES)) {
                                        showDialog(context, context.getResources().getString(R.string.dialog_versions_source_stable_releases_url_invaild_title), context.getResources().getString(R.string.dialog_versions_source_stable_releases_url_invaild_message));
                                    } else if (versionType.equals(VersionsType.SNAPSHOT_PREVIEW)) {
                                        showDialog(context, context.getResources().getString(R.string.dialog_versions_source_snapshot_preview_url_invaild_title), context.getResources().getString(R.string.dialog_versions_source_snapshot_preview_url_invaild_message));
                                    } else if (versionType.equals(VersionsType.BETA)) {
                                        showDialog(context, context.getResources().getString(R.string.dialog_versions_source_beta_url_invaild_title), context.getResources().getString(R.string.dialog_versions_source_beta_url_invaild_message));
                                    } else if (versionType.equals(VersionsType.ALPHA)) {
                                        showDialog(context, context.getResources().getString(R.string.dialog_versions_source_alpha_url_invaild_title), context.getResources().getString(R.string.dialog_versions_source_alpha_url_invaild_message));
                                    }
                                    Looper.loop();
                                }
                            }
                        }, 0, 100);

                        waitDownload.dismiss();
                    } catch (IOException e) {
                        Looper.prepare();
                        exceptionDialog(context, e, context.getResources().getString(R.string.dialog_exception_downloadfailed));
                        Looper.loop();
                    }

                }
            }).start();
        } else {
            Toast.makeText(context, context.getResources().getString(R.string.toast_please_check_your_network), Toast.LENGTH_SHORT).show();
        }
    }
}
