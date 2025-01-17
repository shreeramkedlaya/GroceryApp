package com.example.groceryapp;

import android.widget.Filter;

import java.util.ArrayList;

public class FilterProduct extends Filter {
    private AdapterProductSeller adapter;
    private ArrayList<ModelProduct> filterList;

    public FilterProduct(ArrayList<ModelProduct> filterList, AdapterProductSeller adapter) {
        this.filterList = filterList;
        this.adapter = adapter;
    }

    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
        FilterResults results = new FilterResults();
        // validate data for search query
        if (constraint != null && constraint.length() > 0) {
            // search field not empty, searching something, perform search

            // change to uppercase, to make case insensitive
            constraint=constraint.toString().toUpperCase();
            // store our filtered list
            ArrayList<ModelProduct> filteredModels=new ArrayList<>();
            for (int i=0;i<filterList.size();i++) {
                // check, search by title and category
                if(filterList.get(i).getProductTitle().toUpperCase().contains(constraint) ||
                        filterList.get(i).getProductCategory().toUpperCase().contains(constraint) ) {
                    // add filtered data to list
                    filteredModels.add(filterList.get(i));
                }
            }
            results.count=filteredModels.size();
            results.values=filteredModels;

        }
        else {
            // search field empty, not searching, return original/all/complete list
            results.count=filterList.size();
            results.values=filterList;
        }
        return results;
    }

    @Override
    protected void publishResults(CharSequence constraint, FilterResults results) {
        adapter.productList=(ArrayList<ModelProduct>) results.values;
        // refresh adapter
        adapter.notifyDataSetChanged();
    }
}
