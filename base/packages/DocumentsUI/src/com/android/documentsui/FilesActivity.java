/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.documentsui;

import static com.android.documentsui.OperationDialogFragment.DIALOG_TYPE_UNKNOWN;
import static com.android.documentsui.Shared.DEBUG;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.DocumentsContract;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import com.android.documentsui.OperationDialogFragment.DialogType;
import com.android.documentsui.RecentsProvider.ResumeColumns;
import com.android.documentsui.dirlist.AnimationView;
import com.android.documentsui.dirlist.DirectoryFragment;
import com.android.documentsui.dirlist.Model;
import com.android.documentsui.model.DocumentInfo;
import com.android.documentsui.model.DocumentStack;
import com.android.documentsui.model.DurableUtils;
import com.android.documentsui.model.RootInfo;
import com.android.documentsui.services.FileOperationService;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import android.Manifest;

import android.content.ContentUris;
import android.content.Context;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import java.io.File;

import android.content.pm.PackageManager;
/**
 * Standalone file management activity.
 */
public class FilesActivity extends BaseActivity {

    public static final String TAG = "FilesActivity";

    // See comments where this const is referenced for details.
    private static final int DRAWER_NO_FIDDLE_DELAY = 1500;

    // Track the time we opened the drawer in response to back being pressed.
    // We use the time gap to figure out whether to close app or reopen the drawer.
    private long mDrawerLastFiddled;
    private DocumentClipper mClipper;

    public FilesActivity() {
        super(R.layout.files_activity, TAG);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
		Log.i("xiao","intent ==null====xxxx=====FilesActivity===");
        mClipper = new DocumentClipper(this);

        RootsFragment.show(getFragmentManager(), null);

        final Intent intent = getIntent();
        final Uri uri = intent.getData();

        if (mState.restored) {
            if (DEBUG) Log.d(TAG, "Stack already resolved for uri: " + intent.getData());
        } else if (!mState.stack.isEmpty()) {
            // If a non-empty stack is present in our state, it was read (presumably)
            // from EXTRA_STACK intent extra. In this case, we'll skip other means of
            // loading or restoring the stack (like URI).
            //
            // When restoring from a stack, if a URI is present, it should only ever be:
            // -- a launch URI: Launch URIs support sensible activity management,
            //    but don't specify a real content target)
            // -- a fake Uri from notifications. These URIs have no authority (TODO: details).
            //
            // Any other URI is *sorta* unexpected...except when browsing an archive
            // in downloads.
            if(uri != null
                    && uri.getAuthority() != null
                    && !uri.equals(mState.stack.peek())
                    && !LauncherActivity.isLaunchUri(uri)) {
                if (DEBUG) Log.w(TAG,
                        "Launching with non-empty stack. Ignoring unexpected uri: " + uri);
            } else {
                if (DEBUG) Log.d(TAG, "Launching with non-empty stack.");
            }
            refreshCurrentRootAndDirectory(AnimationView.ANIM_NONE);
        } else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            assert(uri != null);
            new OpenUriForViewTask(this).executeOnExecutor(
                    ProviderExecutor.forAuthority(uri.getAuthority()), uri);
        } else if (DocumentsContract.isRootUri(this, uri)) {
            if (DEBUG) Log.d(TAG, "Launching with root URI.");
            // If we've got a specific root to display, restore that root using a dedicated
            // authority. That way a misbehaving provider won't result in an ANR.
            loadRoot(uri);
        } else {
            if (DEBUG) Log.d(TAG, "All other means skipped. Launching into default directory.");
            loadRoot(getDefaultRoot());
        }

        final @DialogType int dialogType = intent.getIntExtra(
                FileOperationService.EXTRA_DIALOG_TYPE, DIALOG_TYPE_UNKNOWN);
        // DialogFragment takes care of restoring the dialog on configuration change.
        // Only show it manually for the first time (icicle is null).
        if (icicle == null && dialogType != DIALOG_TYPE_UNKNOWN) {
            final int opType = intent.getIntExtra(
                    FileOperationService.EXTRA_OPERATION,
                    FileOperationService.OPERATION_COPY);
            final ArrayList<DocumentInfo> srcList =
                    intent.getParcelableArrayListExtra(FileOperationService.EXTRA_SRC_LIST);
            OperationDialogFragment.show(
                    getFragmentManager(),
                    dialogType,
                    srcList,
                    mState.stack,
                    opType);
        }
		if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
			requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
		}
		
    }

    @Override
    void includeState(State state) {
        final Intent intent = getIntent();

        state.action = State.ACTION_BROWSE;
        state.allowMultiple = true;

        // Options specific to the DocumentsActivity.
        assert(!intent.hasExtra(Intent.EXTRA_LOCAL_ONLY));

        final DocumentStack stack = intent.getParcelableExtra(Shared.EXTRA_STACK);
        if (stack != null) {
            state.stack = stack;
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // This check avoids a flicker from "Recents" to "Home".
        // Only update action bar at this point if there is an active
        // serach. Why? Because this avoid an early (undesired) load of
        // the recents root...which is the default root in other activities.
        // In Files app "Home" is the default, but it is loaded async.
        // update will be called once Home root is loaded.
        // Except while searching we need this call to ensure the
        // search bits get layed out correctly.
        if (mSearchManager.isSearching()) {
            mNavigator.update();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        final RootInfo root = getCurrentRoot();

        // If we're browsing a specific root, and that root went away, then we
        // have no reason to hang around.
        // TODO: Rather than just disappearing, maybe we should inform
        // the user what has happened, let them close us. Less surprising.
        if (mRoots.getRootBlocking(root.authority, root.rootId) == null) {
            finish();
        }
    }

    @Override
    public String getDrawerTitle() {
        Intent intent = getIntent();
        return (intent != null && intent.hasExtra(Intent.EXTRA_TITLE))
                ? intent.getStringExtra(Intent.EXTRA_TITLE)
                : getTitle().toString();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        final RootInfo root = getCurrentRoot();

        final MenuItem createDir = menu.findItem(R.id.menu_create_dir);
        final MenuItem pasteFromCb = menu.findItem(R.id.menu_paste_from_clipboard);
        final MenuItem settings = menu.findItem(R.id.menu_settings);
        final MenuItem newWindow = menu.findItem(R.id.menu_new_window);

        createDir.setVisible(true);
        createDir.setEnabled(canCreateDirectory());
        pasteFromCb.setEnabled(mClipper.hasItemsToPaste());
        settings.setVisible(root.hasSettings());
        newWindow.setVisible(Shared.shouldShowFancyFeatures(this));

        Menus.disableHiddenItems(menu, pasteFromCb);
        // It hides icon if searching in progress
        mSearchManager.updateMenu();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_create_dir:
                assert(canCreateDirectory());
                showCreateDirectoryDialog();
                break;
            case R.id.menu_new_window:
                createNewWindow();
                break;
            case R.id.menu_paste_from_clipboard:
                DirectoryFragment dir = getDirectoryFragment();
                if (dir != null) {
                    dir.pasteFromClipboard();
                }
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void createNewWindow() {
        Metrics.logUserAction(this, Metrics.USER_ACTION_NEW_WINDOW);

        Intent intent = LauncherActivity.createLaunchIntent(this);
        intent.putExtra(Shared.EXTRA_STACK, (Parcelable) mState.stack);

        // With new multi-window mode we have to pick how we are launched.
        // By default we'd be launched in-place above the existing app.
        // By setting launch-to-side ActivityManager will open us to side.
        if (isInMultiWindowMode()) {
            intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);
        }

        startActivity(intent);
    }

    @Override
    void refreshDirectory(int anim) {
        final FragmentManager fm = getFragmentManager();
        final RootInfo root = getCurrentRoot();
        final DocumentInfo cwd = getCurrentDirectory();

        assert(!mSearchManager.isSearching());

        if (cwd == null) {
            DirectoryFragment.showRecentsOpen(fm, anim);
        } else {
            // Normal boring directory
            DirectoryFragment.showDirectory(fm, root, cwd, anim);
        }
    }

    @Override
    void onRootPicked(RootInfo root) {
        super.onRootPicked(root);
        mDrawer.setOpen(false);
    }

    @Override
    public void onDocumentsPicked(List<DocumentInfo> docs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onDocumentPicked(DocumentInfo doc, Model model) {
        // Anything on downloads goes through the back through downloads manager
        // (that's the MANAGE_DOCUMENT bit).
        // This is done for two reasons:
        // 1) The file in question might be a failed/queued or otherwise have some
        //    specialized download handling.
        // 2) For APKs, the download manager will add on some important security stuff
        //    like origin URL.
        // All other files not on downloads, event APKs, would get no benefit from this
        // treatment, thusly the "isDownloads" check.

        // Launch MANAGE_DOCUMENTS only for the root level files, so it's not called for
        // files in archives. Also, if the activity is already browsing a ZIP from downloads,
        // then skip MANAGE_DOCUMENTS.
        final boolean isViewing = Intent.ACTION_VIEW.equals(getIntent().getAction());
        final boolean isInArchive = mState.stack.size() > 1;

        if (getCurrentRoot().isDownloads() && !isInArchive && !isViewing) {
            // First try managing the document; we expect manager to filter
            // based on authority, so we don't grant.
            final Intent manage = new Intent(DocumentsContract.ACTION_MANAGE_DOCUMENT);
            manage.setData(doc.derivedUri);
            try {
                startActivity(manage);
                return;
            } catch (ActivityNotFoundException ex) {
                // fall back to regular handling below.
            }
        }

        if (doc.isContainer()) {
            openContainerDocument(doc);
        } else {
            openDocument(doc, model);
        }
    }

    /**
     * Launches an intent to view the specified document.
     */
    private void openDocument(DocumentInfo doc, Model model) {
        Intent intent = new QuickViewIntentBuilder(
                getPackageManager(), getResources(), doc, model).build();

        if (intent != null) {
            // TODO: un-work around issue b/24963914. Should be fixed soon.
            try {
                startActivity(intent);
                return;
            } catch (SecurityException e) {
                // Carry on to regular view mode.
                Log.e(TAG, "Caught security error: " + e.getLocalizedMessage());
            }
        }
        // Fall back to traditional VIEW action...
        intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if(doc.mimeType !=null && doc.mimeType.startsWith("audio")){
			String filePath = getPath(this,doc.derivedUri);
			if(filePath != null && !"".equals(filePath)){
				File docFile = new File(filePath);
				Uri docUri = Uri.fromFile(docFile);
				if(docUri != null){
					Log.i("xiao","intent:docUri="+docUri);
					intent.setDataAndType(docUri,doc.mimeType);
				}else{
					intent.setData(doc.derivedUri);	
				}
			}else{
				intent.setData(doc.derivedUri);	
			}	
		}else{
			intent.setData(doc.derivedUri);	
		}
		Log.i("xiao","intent:doc.mimeType="+doc.mimeType+"=======openDocument==="+doc.derivedUri);
        if (DEBUG && intent.getClipData() != null) {
            Log.d(TAG, "Starting intent w/ clip data: " + intent.getClipData());
        }

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Snackbars.makeSnackbar(
                    this, R.string.toast_no_application, Snackbar.LENGTH_SHORT).show();
        }
    }
	//xiaoyuguang
	  /**
 * Get a file path from a Uri. This will get the the path for Storage Access
 * Framework Documents, as well as the _data field for the MediaStore and
 * other file-based ContentProviders.
 *
 * @param context The context.
 * @param uri The Uri to query.
 * @author paulburke
 */
public static String getPath(final Context context, final Uri uri) {
	Log.e("fangshiquan","getPath方法");
    final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

    // DocumentProvider
    if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
        // ExternalStorageProvider
        if (isExternalStorageDocument(uri)) {
            final String docId = DocumentsContract.getDocumentId(uri);
            final String[] split = docId.split(":");
            final String type = split[0];

            if ("primary".equalsIgnoreCase(type)) {
                return Environment.getExternalStorageDirectory() + "/" + split[1];
            }else{
				return "/storage/"+type+"/"+split[1];
			}
            // TODO handle non-primary volumes
        }
        // DownloadsProvider
        else if (isDownloadsDocument(uri)) {

            final String id = DocumentsContract.getDocumentId(uri);
            final Uri contentUri = ContentUris.withAppendedId(
                    Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

            return getDataColumn(context, contentUri, null, null);
        }
        // MediaProvider
        else if (isMediaDocument(uri)) {
            final String docId = DocumentsContract.getDocumentId(uri);
            final String[] split = docId.split(":");
            final String type = split[0];

            Uri contentUri = null;
            if ("image".equals(type)) {
                contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            } else if ("video".equals(type)) {
                contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            } else if ("audio".equals(type)) {
                contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            }

            final String selection = "_id=?";
            final String[] selectionArgs = new String[] {
                    split[1]
            };

            return getDataColumn(context, contentUri, selection, selectionArgs);
        }
    }
    // MediaStore (and general)
    else if ("content".equalsIgnoreCase(uri.getScheme())) {
        return getDataColumn(context, uri, null, null);
    }
    // File
    else if ("file".equalsIgnoreCase(uri.getScheme())) {
        return uri.getPath();
    }

    return null;
}

/**
 * Get the value of the data column for this Uri. This is useful for
 * MediaStore Uris, and other file-based ContentProviders.
 *
 * @param context The context.
 * @param uri The Uri to query.
 * @param selection (Optional) Filter used in the query.
 * @param selectionArgs (Optional) Selection arguments used in the query.
 * @return The value of the _data column, which is typically a file path.
 */
public static String getDataColumn(Context context, Uri uri, String selection,
        String[] selectionArgs) {
	Log.e("fangshiquan","getDataColumn方法");
    Cursor cursor = null;
    final String column = "_data";
    final String[] projection = {
            column
    };

    try {
		
        cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                null);
		
        if (cursor != null && cursor.moveToFirst()) {
            final int column_index = cursor.getColumnIndexOrThrow(column);
            return cursor.getString(column_index);
        }
    } finally {
        if (cursor != null)
            cursor.close();
    }
    return null;
}


/**
 * @param uri The Uri to check.
 * @return Whether the Uri authority is ExternalStorageProvider.
 */
public static boolean isExternalStorageDocument(Uri uri) {
    return "com.android.externalstorage.documents".equals(uri.getAuthority());
}

/**
 * @param uri The Uri to check.
 * @return Whether the Uri authority is DownloadsProvider.
 */
public static boolean isDownloadsDocument(Uri uri) {
    return "com.android.providers.downloads.documents".equals(uri.getAuthority());
}

/**
 * @param uri The Uri to check.
 * @return Whether the Uri authority is MediaProvider.
 */
public static boolean isMediaDocument(Uri uri) {
    return "com.android.providers.media.documents".equals(uri.getAuthority());
}
	//end xiaoyuguang
    @Override
    public boolean onKeyShortcut(int keyCode, KeyEvent event) {
        DirectoryFragment dir;
        // TODO: All key events should be statically bound using alphabeticShortcut.
        // But not working.
        switch (keyCode) {
            case KeyEvent.KEYCODE_A:
                dir = getDirectoryFragment();
                if (dir != null) {
                    dir.selectAllFiles();
                }
                return true;
            case KeyEvent.KEYCODE_C:
                dir = getDirectoryFragment();
                if (dir != null) {
                    dir.copySelectedToClipboard();
                }
                return true;
            case KeyEvent.KEYCODE_V:
                dir = getDirectoryFragment();
                if (dir != null) {
                    dir.pasteFromClipboard();
                }
                return true;
            default:
                return super.onKeyShortcut(keyCode, event);
        }
    }

    // Do some "do what a I want" drawer fiddling, but don't
    // do it if user already hit back recently and we recently
    // did some fiddling.
    @Override
    boolean onBeforePopDir() {
        int size = mState.stack.size();

        if (mDrawer.isPresent()
                && (System.currentTimeMillis() - mDrawerLastFiddled) > DRAWER_NO_FIDDLE_DELAY) {
            // Close drawer if it is open.
            if (mDrawer.isOpen()) {
                mDrawer.setOpen(false);
                mDrawerLastFiddled = System.currentTimeMillis();
                return true;
            }

            // Open the Close drawer if it is closed and we're at the top of a root.
            if (size <= 1) {
                mDrawer.setOpen(true);
                // Remember so we don't just close it again if back is pressed again.
                mDrawerLastFiddled = System.currentTimeMillis();
                return true;
            }
        }

        return false;
    }

    // Turns out only DocumentsActivity was ever calling saveStackBlocking.
    // There may be a  case where we want to contribute entries from
    // Behavior here in FilesActivity, but it isn't yet obvious.
    // TODO: Contribute to recents, or remove this.
    void writeStackToRecentsBlocking() {
        final ContentResolver resolver = getContentResolver();
        final ContentValues values = new ContentValues();

        final byte[] rawStack = DurableUtils.writeToArrayOrNull(mState.stack);

        // Remember location for next app launch
        final String packageName = getCallingPackageMaybeExtra();
        values.clear();
        values.put(ResumeColumns.STACK, rawStack);
        values.put(ResumeColumns.EXTERNAL, 0);
        resolver.insert(RecentsProvider.buildResume(packageName), values);
    }

    @Override
    void onTaskFinished(Uri... uris) {
        if (DEBUG) Log.d(TAG, "onFinished() " + Arrays.toString(uris));

        final Intent intent = new Intent();
        if (uris.length == 1) {
            intent.setData(uris[0]);
        } else if (uris.length > 1) {
            final ClipData clipData = new ClipData(
                    null, mState.acceptMimes, new ClipData.Item(uris[0]));
            for (int i = 1; i < uris.length; i++) {
                clipData.addItem(new ClipData.Item(uris[i]));
            }
            intent.setClipData(clipData);
        }

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    /**
     * Builds a stack for the specific Uris. Multi roots are not supported, as it's impossible
     * to know which root to select. Also, the stack doesn't contain intermediate directories.
     * It's primarly used for opening ZIP archives from Downloads app.
     */
    private static final class OpenUriForViewTask extends PairedTask<FilesActivity, Uri, Void> {

        private final State mState;
        public OpenUriForViewTask(FilesActivity activity) {
            super(activity);
            mState = activity.mState;
        }

        @Override
        protected Void run(Uri... params) {
            final Uri uri = params[0];

            final RootsCache rootsCache = DocumentsApplication.getRootsCache(mOwner);
            final String authority = uri.getAuthority();

            final Collection<RootInfo> roots =
                    rootsCache.getRootsForAuthorityBlocking(authority);
            if (roots.isEmpty()) {
                Log.e(TAG, "Failed to find root for the requested Uri: " + uri);
                return null;
            }

            final RootInfo root = roots.iterator().next();
            mState.stack.root = root;
            try {
                mState.stack.add(DocumentInfo.fromUri(mOwner.getContentResolver(), uri));
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Failed to resolve DocumentInfo from Uri: " + uri);
            }
            mState.stack.add(mOwner.getRootDocumentBlocking(root));
            return null;
        }

        @Override
        protected void finish(Void result) {
            mOwner.refreshCurrentRootAndDirectory(AnimationView.ANIM_NONE);
        }
    }
}
