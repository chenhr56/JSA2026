data = readmatrix(strcat('data/', "HV" ,'.txt'));

[No_of_cases, linkage_index] = size(data)

linkage = data(:,linkage_index);

for n = 1 : linkage_index-1
   [p] = ranksum(linkage, data(:,n))
   disp(p)
end