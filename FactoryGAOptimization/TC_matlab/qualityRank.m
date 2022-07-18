close all;

path_figs = 'fig/';
path_data = 'data/';
data_file = ["DCI","IGD","HV"];
y_label = ["DCI","IGD","HV"];

% colors=[[0.8500 0.3250 0.0980]; [0 0.4470 0.7410];  [0.9290 0.6940 0.1250]; [0.4660, 0.6740, 0.1880]; [0.3010, 0.7450, 0.9330]; [0.6350, 0.0780, 0.1840]];
colors = [[0 0.4470 0.7410]; [0.8500 0.3250 0.0980]; [0.9290 0.6940 0.1250]; [0.4940 0.1840 0.5560]; [0.4660 0.6740 0.1880]; [0.8500, 0.3250, 0.0980]; [1 1 1]];
colors1 = [[0 0 0]; [0 0 0]; [0 0 0]; [0 0 0]; [0 0 0]; [0 0 0]; [0 0 0]];

for n = 1 : length(data_file)
    
    f=figure('Position', [100, 100, 1000, 400]);
    set(f,'defaultAxesColorOrder',[[0,60/255,255/255];[0,0,0]]);

    data = readmatrix(strcat(path_data, data_file(n) ,'.txt'));
    
    [row, col] = size(data);

    if col == 11
        line([1.5 1.5],[-0.5 11.5],'Color','black','LineStyle',':')
        hold on
        line([2.5 2.5],[-0.5 11.5],'Color','black','LineStyle',':')
        hold on
        line([6.5 6.5],[-0.5 11.5],'Color','black','LineStyle',':')
        hold on
        line([10.5 10.5],[-0.5 11.5],'Color','black','LineStyle',':')
        hold on
    end
    
    if col == 12
        line([1.5 1.5],[-1 12.5],'Color','black','LineStyle',':')
        hold on
        line([2.5 2.5],[-1 12.5],'Color','black','LineStyle',':')
        hold on
        line([3.5 3.5],[-1 12.5],'Color','black','LineStyle',':')
        hold on
        line([7.5 7.5],[-1 12.5],'Color','black','LineStyle',':')
        hold on
        line([11.5 11.5],[-1 12.5],'Color','black','LineStyle',':')
        hold on
    end
    
    for i = 1:col
        boxplot(data(:,i), 'position', i, 'widths', 0.65, 'color', 'k');
        hold on
    end
%     if col == 11
%         boxplot(data(:,1), 'position', 1, 'widths', 0.65, 'color', 'k');
%         hold on
%         boxplot(data(:,2), 'position', 2, 'widths', 0.65, 'color', colors1(6,:));
%         hold on
%         boxplot(data(:,3), 'position', 3, 'widths', 0.65, 'color', colors1(5,:));
%         hold on
%         boxplot(data(:,4), 'position', 4, 'widths', 0.65, 'color', colors1(4,:));
%         hold on
%         boxplot(data(:,5), 'position', 5, 'widths', 0.65, 'color', colors1(3,:));
%         hold on
%         boxplot(data(:,6), 'position', 6, 'widths', 0.65, 'color', colors1(2,:));
%         hold on
%         boxplot(data(:,7), 'position', 7, 'widths', 0.65, 'color', colors1(5,:));
%         hold on
%         boxplot(data(:,8), 'position', 8, 'widths', 0.65, 'color', colors1(4,:));
%         hold on
%         boxplot(data(:,9), 'position', 9, 'widths', 0.65, 'color', colors1(3,:));
%         hold on
%         boxplot(data(:,10), 'position', 10, 'widths', 0.65, 'color', colors1(2,:));
%         hold on
%         boxplot(data(:,11), 'position', 11, 'widths', 0.65, 'color', colors1(1,:));
%     end
%     if col == 12
%         boxplot(data(:,1), 'position', 1, 'widths', 0.65, 'color', 'k');
%         hold on
%         boxplot(data(:,2), 'position', 2, 'widths', 0.65, 'color', 'k');
%         hold on
%         boxplot(data(:,3), 'position', 3, 'widths', 0.65, 'color', colors1(6,:));
%         hold on
%         boxplot(data(:,4), 'position', 4, 'widths', 0.65, 'color', colors1(5,:));
%         hold on
%         boxplot(data(:,5), 'position', 5, 'widths', 0.65, 'color', colors1(4,:));
%         hold on
%         boxplot(data(:,6), 'position', 6, 'widths', 0.65, 'color', colors1(3,:));
%         hold on
%         boxplot(data(:,7), 'position', 7, 'widths', 0.65, 'color', colors1(2,:));
%         hold on
%         boxplot(data(:,8), 'position', 8, 'widths', 0.65, 'color', colors1(5,:));
%         hold on
%         boxplot(data(:,9), 'position', 9, 'widths', 0.65, 'color', colors1(4,:));
%         hold on
%         boxplot(data(:,10), 'position', 10, 'widths', 0.65, 'color', colors1(3,:));
%         hold on
%         boxplot(data(:,11), 'position', 11, 'widths', 0.65, 'color', colors1(2,:));
%         hold on
%         boxplot(data(:,12), 'position', 12, 'widths', 0.65, 'color', colors1(1,:));
%     end

    ylabel(strcat(y_label(n),' rank'),'FontSize', 18)
    
    if col == 11
        ylim([0 11.5]);
        xlim([0.5 11.5]);
        
        set(gca,'xtick',[1 2 4.5 8.5 11]);
        set(gca,'xticklabel',{'MOEA/D', 'No-Mig', 'Replace random','Replace worst', 'linkage'},'FontSize', 12);
    end
    if col == 12
        ylim([-0.8 12.5]);
        xlim([0.5 12.5]);
        
        set(gca,'xtick',[1 2 3 5.5 9.5 12]);
        set(gca,'xticklabel',{'NSGA-II', 'MOEA/D', 'No-Mig', 'Replace random','Replace worst', 'linkage'},'FontSize', 12);
    end
    xlabel('Migration strategies','FontSize', 14)


    colordegree = 0.5;

    h = findobj(gca,'Tag','Box');
    patch(get(h(1),'XData'),get(h(1),'YData'), colors(6,:),'FaceAlpha',colordegree);
    patch(get(h(2),'XData'),get(h(2),'YData'), colors(5,:),'FaceAlpha',colordegree);
    patch(get(h(3),'XData'),get(h(3),'YData'), colors(4,:),'FaceAlpha',colordegree);
    patch(get(h(4),'XData'),get(h(4),'YData'), colors(3,:),'FaceAlpha',colordegree);
    patch(get(h(5),'XData'),get(h(5),'YData'), colors(1,:),'FaceAlpha',colordegree);
    patch(get(h(6),'XData'),get(h(6),'YData'), colors(5,:),'FaceAlpha',colordegree);
    patch(get(h(7),'XData'),get(h(7),'YData'), colors(4,:),'FaceAlpha',colordegree);
    patch(get(h(8),'XData'),get(h(8),'YData'), colors(3,:),'FaceAlpha',colordegree);
    patch(get(h(9),'XData'),get(h(9),'YData'), colors(1,:),'FaceAlpha',colordegree);
%     patch(get(h(10),'XData'),get(h(10),'YData'), colors(1,:),'FaceAlpha',colordegree);
%     patch(get(h(11),'XData'),get(h(11),'YData'), 'k','FaceAlpha',colordegree);
%     if(col == 12)
%         patch(get(h(12),'XData'),get(h(12),'YData'), 'k','FaceAlpha',colordegree);    
%     end


    c = get(gca, 'Children');
    if col == 11
        hleg1 = legend(c([1:6,11]),'MOEA/D', 'No-Mig','new', 'random', 'best', 'diverse','linkage','Location','southeast', 'Orientation','horizontal','FontSize',12);
    end
    if col == 12
        hleg1 = legend(c([1:4,9]),'new', 'random', 'best', 'diverse','linkage','Location','southeast', 'Orientation','horizontal','FontSize',12);
    end

    saveas(gcf,strcat(path_figs,data_file(n),'.eps'),'epsc');

end