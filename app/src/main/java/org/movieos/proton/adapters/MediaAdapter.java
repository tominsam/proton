package org.movieos.proton.adapters;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.movieos.proton.ELog;
import org.movieos.proton.R;
import org.movieos.proton.databinding.MediaHolderBinding;

import java.io.File;

import static com.google.android.gms.internal.zzs.TAG;

public class MediaAdapter extends RecyclerView.Adapter<MediaAdapter.MediaHolder> {

    private final Cursor mImageCursor;
    private final int mImageIdIndex;
    private final int mDataIndex;
    private MediaTappedListener mMediaTappedListener;

    public MediaAdapter(Context context, MediaTappedListener listener) {
        final String[] projection = {
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media._ID
        };
        mImageCursor = context.getContentResolver().query( MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection, // Which columns to return
            null, // no filter
            null,
            MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC");
        assert mImageCursor != null;
        mImageIdIndex = mImageCursor.getColumnIndex(MediaStore.Images.Media._ID);
        mDataIndex = mImageCursor.getColumnIndex(MediaStore.Images.Media.DATA);
        mMediaTappedListener = listener;
        setHasStableIds(true);
    }

    @Override
    public long getItemId(final int position) {
        mImageCursor.moveToPosition(position);
        return mImageCursor.getInt(mImageIdIndex);
    }

    @Override
    public MediaHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        return MediaHolder.create(parent);
    }

    @Override
    public void onBindViewHolder(final MediaHolder holder, final int position) {
        mImageCursor.moveToPosition(position);
        final int imageId = mImageCursor.getInt(mImageIdIndex);
        final String path = mImageCursor.getString(mDataIndex);

        holder.bind(imageId);
        holder.itemView.setOnClickListener(v -> {
            // I suspect this is wrong, but I need more evidence.
            if (mMediaTappedListener != null) {
                mMediaTappedListener.onMediaTapped(Uri.fromFile(new File(path)));
            }
        });
    }

    @Override
    public int getItemCount() {
        return mImageCursor.getCount();
    }

    public void setMediaTappedListener(final MediaTappedListener mediaTappedListener) {
        mMediaTappedListener = mediaTappedListener;
    }

    public interface MediaTappedListener {
        void onMediaTapped(Uri contentUri);
    }

    static class MediaHolder extends RecyclerView.ViewHolder {
        private final MediaHolderBinding mBinding;

        static MediaHolder create(ViewGroup parent) {
            final MediaHolderBinding binding = MediaHolderBinding.inflate(LayoutInflater.from(parent.getContext()));
            return new MediaHolder(binding);
        }

        MediaHolder(final MediaHolderBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
        }

        void bind(final int bitmap) {
            ImageLoaderTask.loadImage(mBinding.icon, bitmap);
        }
    }

    private static class ImageLoaderTask extends AsyncTask<Integer, Void, Bitmap> {
        ImageView mImageView;
        Context mContext;

        static void loadImage(ImageView imageView, int imageId) {
            // show placeholder while loading
            Drawable placeHolder = imageView.getContext().getDrawable(R.drawable.ic_photo_black_24dp).mutate();
            placeHolder.setTint(0xFF444444);
            imageView.setScaleType(ImageView.ScaleType.CENTER);
            imageView.setImageDrawable(placeHolder);

            // Load images in parallel
            new ImageLoaderTask(imageView).executeOnExecutor(THREAD_POOL_EXECUTOR, imageId);
        }

        private ImageLoaderTask(final ImageView imageView) {
            mImageView = imageView;
            mContext = imageView.getContext();

            // We're going to track the ID of the image we're trying to load,
            // so that if the user scrolls quickly we won't load the old
            // image into the holder that now expects something different.
            // If we _do_ re-use the image, cancel any pending thumbnail task
            // currently running on it.
            //
            // This isn't perfect. You can still crash slower devices by flinging
            // the recyclerview a lot. TODO.
            if (mImageView.getTag() instanceof AsyncTask) {
                ELog.i(TAG, "cancelling old task");
                ((AsyncTask) mImageView.getTag()).cancel(true);
            }
            mImageView.setTag(this);
        }

        @Override
        protected Bitmap doInBackground(final Integer... params) {
            try {
                ContentResolver cr = mContext.getContentResolver();
                // this will block if needed while it generates the thumbnail
                return MediaStore.Images.Thumbnails.getThumbnail(cr, params[0], MediaStore.Images.Thumbnails.MINI_KIND, null);
            } catch (Exception e) {
                // task probably cancelled
                return null;
            }
        }

        @Override
        protected void onPostExecute(final Bitmap bitmap) {
            if (mImageView.getTag().equals(this)) {
                // only write image if we're still trying to load this particular image.
                mImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                mImageView.setImageBitmap(bitmap);
                mImageView.setTag(null);
            }
        }
    }

}
