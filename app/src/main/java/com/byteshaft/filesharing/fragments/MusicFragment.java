package com.byteshaft.filesharing.fragments;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.TextView;

import com.byteshaft.filesharing.R;
import com.byteshaft.filesharing.activities.ActivitySendFile;
import com.byteshaft.filesharing.utils.Helpers;
import com.github.siyamed.shapeimageview.CircularImageView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;


public class MusicFragment extends Fragment {

    private ArrayList<String> musicList;
    private Adapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.music_fragment, container, false);
        musicList = new ArrayList<>();
        GridView gridLayout = (GridView) rootView.findViewById(R.id.music_grid);
        adapter = new Adapter(getActivity().getApplicationContext(),
                R.layout.delegate_photo_fragment, musicList);
        gridLayout.setAdapter(adapter);
        gridLayout.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                CheckBox musicCheckbox = (CheckBox) view.findViewById(R.id.music_checkbox);
                File file = new File(musicList.get(i));
                String filePath = file.getAbsolutePath();
                HashMap<String, String> fileItem = Helpers.getFileMetadataMap(
                        file.getAbsolutePath(), "music");
                if (!ActivitySendFile.sendList.containsKey(filePath)) {
                    ActivitySendFile.sendList.put(filePath, fileItem);
                    musicCheckbox.setChecked(true);
                    musicCheckbox.setVisibility(View.VISIBLE);
                } else {
                    ActivitySendFile.sendList.remove(filePath);
                    musicCheckbox.setChecked(false);
                    musicCheckbox.setVisibility(View.GONE);
                }
                ActivitySendFile.getInstance().setSelection();

            }
        });
        getAudioList();
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
    }

    private void getAudioList() {
        String[] acceptedExtensions = {
                "mp3", "mp2", "wav", "flac", "ogg", "au", "snd", "mid", "midi", "kar",
                "mga", "aif", "aiff", "aifc", "m3u", "oga", "spx"
        };
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        //your projection statement
        Cursor cursor;
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID
        };
        //query
        cursor = getActivity().getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null);


        while (cursor.moveToNext()) {
            String musicPath = cursor.getString(cursor
                    .getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
            for (String music : acceptedExtensions) {
                if (getFileExt(musicPath).contains(music)) {
                    musicList.add(musicPath);
                    adapter.notifyDataSetChanged();
                }
            }
        }
    }

    public static String getFileExt(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length());
    }

    private class Adapter extends ArrayAdapter<ArrayList<String>> {

        private ArrayList<String> folderList;
        private ViewHolder viewHolder;

        Adapter(Context context, int resource, ArrayList<String> folderList) {
            super(context, resource);
            this.folderList = folderList;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                viewHolder = new ViewHolder();
                convertView = getActivity().getLayoutInflater().inflate(R.layout.delegate_music_fragment, parent, false);
                viewHolder.folderImage = (CircularImageView) convertView.findViewById(R.id.music_image);
                viewHolder.musicName = (TextView) convertView.findViewById(R.id.song_name);
                viewHolder.musicCheckbox = (CheckBox) convertView.findViewById(R.id.music_checkbox);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            File f = new File(folderList.get(position));
            if (ActivitySendFile.sendList.containsKey(f.getAbsolutePath())) {
                viewHolder.musicCheckbox.setVisibility(View.VISIBLE);
                viewHolder.musicCheckbox.setChecked(true);
            } else {
                viewHolder.musicCheckbox.setVisibility(View.INVISIBLE);
                viewHolder.musicCheckbox.setChecked(false);
            }
            viewHolder.musicName.setText(f.getName());
            return convertView;
        }

        @Override
        public int getCount() {
            return folderList.size();
        }
    }

    private class ViewHolder {
        CircularImageView folderImage;
        TextView musicName;
        CheckBox musicCheckbox;
    }
}
