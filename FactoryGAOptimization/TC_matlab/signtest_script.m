data = readmatrix(strcat('data/', "hv" ,'.txt'));

[No_of_cases, linkage_index] = size(data);

linkage = data(:,linkage_index);

for n = 1 : linkage_index-1
   [p,h] = ranksum(linkage, data(:,n))
   disp(p)
end