close all;

path_figs = 'fig/';
path_data = 'data/';
data_file = ["dci","igd","hv"];
y_label = ["DCI","IGD","HV"];

% colors=[[0.8500 0.3250 0.0980]; [0 0.4470 0.7410];  [0.9290 0.6940 0.1250]; [0.4660, 0.6740, 0.1880]; [0.3010, 0.7450, 0.9330]; [0.6350, 0.0780, 0.1840]];
colors = [[0 0.4470 0.7410]; [0.8500 0.3250 0.0980]; [0.9290 0.6940 0.1250]; [0.4940 0.1840 0.5560]; [0.4660 0.6740 0.1880]; 0.6350 0.0780 0.1840; [1 1 1]];
colors1 = [[0 0 0]; [0 0 0]; [0 0 0]; [0 0 0]; [0 0 0]; [0 0 0]; [0 0 0]];

for n = 1 : length(data_file)
    
    f=figure('Position', [100, 100, 850, 400]);
    set(f,'defaultAxesColorOrder',[[0,60/255,255/255];[0,0,0]]);

    data = readmatrix(strcat(path_data, data_file(n) ,'.txt'));

    line([1.5 1.5],[-0.5 11.5],'Color','black','LineStyle',':')
    hold on    
    line([6.5 6.5],[-0.5 11.5],'Color','black','LineStyle',':')
    hold on
    line([2.5 2.5],[-0.5 11.5],'Color','black','LineStyle',':')
    hold on
    line([10.5 10.5],[-0.5 11.5],'Color','black','LineStyle',':')
    hold on


    boxplot(data(:,1), 'position', 1, 'widths', 0.65, 'color', 'k');
    hold on
    boxplot(data(:,2), 'position', 2, 'widths', 0.65, 'color', colors1(6,:));
    hold on
    boxplot(data(:,3), 'position', 3, 'widths', 0.65, 'color', colors1(5,:));
    hold on
    boxplot(data(:,4), 'position', 4, 'widths', 0.65, 'color', colors1(4,:));
    hold on
    boxplot(data(:,5), 'position', 5, 'widths', 0.65, 'color', colors1(3,:));
    hold on
    boxplot(data(:,6), 'position', 6, 'widths', 0.65, 'color', colors1(2,:));
    hold on
    boxplot(data(:,7), 'position', 7, 'widths', 0.65, 'color', colors1(5,:));
    hold on
    boxplot(data(:,8), 'position', 8, 'widths', 0.65, 'color', colors1(4,:));
    hold on
    boxplot(data(:,9), 'position', 9, 'widths', 0.65, 'color', colors1(3,:));
    hold on
    boxplot(data(:,10), 'position', 10, 'widths', 0.65, 'color', colors1(2,:));
    hold on
    boxplot(data(:,11), 'position', 11, 'widths', 0.65, 'color', colors1(1,:));

    ylabel(strcat(y_label(n),' rank'),'FontSize', 18)
    ylim([-0.8 11.5]);


    xlim([0.5 11.5]);
    set(gca,'xtick',[1 2 4.5 8.5 11]);
    set(gca,'xticklabel',{'MOEA/D', 'No-Mig', 'Replace random','Replace worst', 'linkage'},'FontSize', 12);
    xlabel('Migration strategies','FontSize', 14)


    colordegree = 0.8;

    h = findobj(gca,'Tag','Box');
    patch(get(h(1),'XData'),get(h(1),'YData'), colors(6,:),'FaceAlpha',colordegree);
    patch(get(h(2),'XData'),get(h(2),'YData'), colors(5,:),'FaceAlpha',colordegree);
    patch(get(h(3),'XData'),get(h(3),'YData'), colors(4,:),'FaceAlpha',colordegree);
    patch(get(h(4),'XData'),get(h(4),'YData'), colors(3,:),'FaceAlpha',colordegree);
    patch(get(h(5),'XData'),get(h(5),'YData'), colors(2,:),'FaceAlpha',colordegree);
    patch(get(h(6),'XData'),get(h(6),'YData'), colors(5,:),'FaceAlpha',colordegree);
    patch(get(h(7),'XData'),get(h(7),'YData'), colors(4,:),'FaceAlpha',colordegree);
    patch(get(h(8),'XData'),get(h(8),'YData'), colors(3,:),'FaceAlpha',colordegree);
    patch(get(h(9),'XData'),get(h(9),'YData'), colors(2,:),'FaceAlpha',colordegree);
    patch(get(h(10),'XData'),get(h(10),'YData'), colors(1,:),'FaceAlpha',colordegree);
    patch(get(h(11),'XData'),get(h(11),'YData'), colors(7,:),'FaceAlpha',colordegree);


    c = get(gca, 'Children');
    hleg1 = legend(c([1:6,11]),'MOEA/D', 'No-Mig','new', 'random', 'best', 'diverse','linkage','Location','southeast', 'Orientation','horizontal','FontSize',10);

    saveas(gcf,strcat(path_figs,data_file(n),'.eps'),'epsc');

end